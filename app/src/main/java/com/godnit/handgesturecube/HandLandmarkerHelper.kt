/*
 * Based on the Apache-2.0 MediaPipe Hand Landmarker Android sample.
 * Copyright 2022 The TensorFlow Authors.
 */
package com.godnit.handgesturecube

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.ByteBufferImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.util.concurrent.atomic.AtomicBoolean

class HandLandmarkerHelper(
    private val context: Context,
    private val listener: Listener
) {
    private var handLandmarker: HandLandmarker? = null
    private var reusableBitmap: Bitmap? = null
    private val frameInFlight = AtomicBoolean(false)
    private var resultCount = 0
    private var activeDelegate = "CPU"

    private var pendingWidth = 1
    private var pendingHeight = 1
    private var pendingRotation = 0
    private var pendingMirrorX = false
    private var pendingZeroCopy = false
    private var pendingProxy: ImageProxy? = null
    private var pendingMpImage: MPImage? = null

    fun setup() {
        try {
            handLandmarker = try {
                createLandmarker(Delegate.GPU).also { activeDelegate = "GPU" }
            } catch (gpuError: Throwable) {
                Log.w(TAG, "GPU delegate unavailable; falling back to CPU", gpuError)
                createLandmarker(Delegate.CPU).also { activeDelegate = "CPU" }
            }
            Log.i(TAG, "HAND_TRACKER_READY_$activeDelegate")
            listener.onReady(activeDelegate)
        } catch (error: Throwable) {
            listener.onError("تعذّر تحميل نموذج اليد: ${error.message ?: error.javaClass.simpleName}")
        }
    }

    private fun createLandmarker(delegate: Delegate): HandLandmarker {
        val baseOptions = BaseOptions.builder()
            .setDelegate(delegate)
            .setModelAssetPath(MODEL_PATH)
            .build()
        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumHands(1)
            .setMinHandDetectionConfidence(0.38f)
            // Keep MediaPipe on its cheaper tracking path for as long as the
            // hand is still visible instead of repeatedly running palm detect.
            .setMinHandPresenceConfidence(0.24f)
            .setMinTrackingConfidence(0.16f)
            .setResultListener(::onResult)
            .setErrorListener {
                releaseInFlight()
                listener.onError(it.message ?: "خطأ غير معروف في متتبع اليد")
            }
            .build()
        return HandLandmarker.createFromOptions(context, options)
    }

    fun detect(imageProxy: ImageProxy, frontCamera: Boolean) {
        val detector = handLandmarker
        if (detector == null) {
            imageProxy.close()
            return
        }

        // Never form a queue of old camera frames. The newest frame wins.
        if (!frameInFlight.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val width = imageProxy.width
        val height = imageProxy.height
        val rotation = imageProxy.imageInfo.rotationDegrees.normalizeRotation()
        pendingWidth = if (rotation % 180 == 0) width else height
        pendingHeight = if (rotation % 180 == 0) height else width
        pendingRotation = rotation
        pendingMirrorX = frontCamera
        pendingZeroCopy = false

        val processing = ImageProcessingOptions.builder()
            .setRotationDegrees(rotation)
            .build()

        // CameraX RGBA output is normally one tightly packed direct buffer.
        // Wrap that buffer directly in MPImage so we skip createBitmap() and a
        // complete width*height*4 memory copy on every tracking result.
        val plane = imageProxy.planes.firstOrNull()
        val expectedBytes = width * height * 4
        if (plane != null && plane.pixelStride == 4 && plane.rowStride == width * 4) {
            try {
                val source = plane.buffer.duplicate()
                source.rewind()
                if (source.remaining() >= expectedBytes) {
                    source.limit(expectedBytes)
                    val packed = source.slice()
                    val mpImage = ByteBufferImageBuilder(
                        packed,
                        width,
                        height,
                        MPImage.IMAGE_FORMAT_RGBA
                    ).build()
                    pendingProxy = imageProxy
                    pendingMpImage = mpImage
                    pendingZeroCopy = true
                    detector.detectAsync(mpImage, processing, SystemClock.uptimeMillis())
                    return
                }
            } catch (fastPathError: Throwable) {
                Log.w(TAG, "Zero-copy camera path unavailable; using bitmap fallback", fastPathError)
            }
        }

        // Safe fallback for devices whose RGBA row stride contains padding.
        val bitmap = obtainBitmap(width, height)
        try {
            val buffer = imageProxy.planes[0].buffer
            buffer.rewind()
            bitmap.copyPixelsFromBuffer(buffer)
        } catch (error: Throwable) {
            imageProxy.close()
            releaseInFlight()
            listener.onError("تعذّرت قراءة صورة الكاميرا: ${error.message ?: error.javaClass.simpleName}")
            return
        }
        imageProxy.close()

        val mpImage = BitmapImageBuilder(bitmap).build()
        pendingProxy = null
        pendingMpImage = mpImage
        try {
            detector.detectAsync(mpImage, processing, SystemClock.uptimeMillis())
        } catch (error: Throwable) {
            releaseInFlight()
            listener.onError("تعذّر تحليل صورة الكاميرا: ${error.message ?: error.javaClass.simpleName}")
        }
    }

    private fun obtainBitmap(width: Int, height: Int): Bitmap {
        val current = reusableBitmap
        if (current == null || current.width != width || current.height != height || current.isRecycled) {
            current?.recycle()
            reusableBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
        return reusableBitmap!!
    }

    private fun onResult(result: HandLandmarkerResult, input: MPImage) {
        resultCount++
        if (resultCount == 1 || resultCount == 10) {
            Log.i(TAG, "HAND_TRACKER_RESULT_$resultCount")
        }
        try {
            listener.onResults(
                ResultBundle(
                    result = result,
                    inputWidth = pendingWidth,
                    inputHeight = pendingHeight,
                    inferenceMs = SystemClock.uptimeMillis() - result.timestampMs(),
                    rotationDegrees = pendingRotation,
                    mirrorX = pendingMirrorX,
                    zeroCopy = pendingZeroCopy
                )
            )
        } finally {
            releaseInFlight()
        }
    }

    @Synchronized
    private fun releaseInFlight() {
        try {
            pendingMpImage?.close()
        } catch (_: Throwable) {
        }
        pendingMpImage = null
        try {
            pendingProxy?.close()
        } catch (_: Throwable) {
        }
        pendingProxy = null
        frameInFlight.set(false)
    }

    fun close() {
        releaseInFlight()
        handLandmarker?.close()
        handLandmarker = null
        reusableBitmap?.recycle()
        reusableBitmap = null
    }

    data class ResultBundle(
        val result: HandLandmarkerResult,
        val inputWidth: Int,
        val inputHeight: Int,
        val inferenceMs: Long,
        val rotationDegrees: Int,
        val mirrorX: Boolean,
        val zeroCopy: Boolean
    )

    interface Listener {
        fun onReady(delegateName: String)
        fun onResults(bundle: ResultBundle)
        fun onError(message: String)
    }

    companion object {
        private const val TAG = "HandGestureCube"
        private const val MODEL_PATH = "hand_landmarker.task"

        private fun Int.normalizeRotation(): Int {
            val normalized = ((this % 360) + 360) % 360
            return when {
                normalized < 45 -> 0
                normalized < 135 -> 90
                normalized < 225 -> 180
                normalized < 315 -> 270
                else -> 0
            }
        }

        fun runCompatibilitySelfTest(context: Context) {
            val baseOptions = BaseOptions.builder()
                .setDelegate(Delegate.CPU)
                .setModelAssetPath(MODEL_PATH)
                .build()
            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .setNumHands(1)
                .build()
            val detector = HandLandmarker.createFromOptions(context, options)
            try {
                repeat(3) { index ->
                    val bitmap = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)
                    val input = BitmapImageBuilder(bitmap).build()
                    detector.detect(input)
                    input.close()
                    bitmap.recycle()
                    Log.i(TAG, "HAND_TRACKER_SELF_TEST_${index + 1}")
                }
                Log.i(TAG, "HAND_TRACKER_SELF_TEST_PASSED")
            } finally {
                detector.close()
            }
        }
    }
}
