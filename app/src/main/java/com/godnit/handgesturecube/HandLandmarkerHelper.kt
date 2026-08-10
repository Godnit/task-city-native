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
    private var resultCount = 0
    private val frameInFlight = AtomicBoolean(false)
    private var activeDelegate = "CPU"
    private var pendingWidth = 1
    private var pendingHeight = 1
    private var pendingMirrorX = false

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
            .setMinHandDetectionConfidence(0.40f)
            // In LIVE_STREAM MediaPipe keeps tracking the hand between palm
            // detections. Lower thresholds keep that fast path alive longer so
            // the expensive palm detector is not re-triggered unnecessarily.
            .setMinHandPresenceConfidence(0.28f)
            .setMinTrackingConfidence(0.20f)
            .setResultListener(::onResult)
            .setErrorListener {
                frameInFlight.set(false)
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

        // Never queue old frames. If inference is busy, discard this frame
        // before doing any bitmap allocation/copy work.
        if (!frameInFlight.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val width = imageProxy.width
        val height = imageProxy.height
        val rotation = imageProxy.imageInfo.rotationDegrees
        val bitmap = obtainBitmap(width, height)
        try {
            val buffer = imageProxy.planes[0].buffer
            buffer.rewind()
            bitmap.copyPixelsFromBuffer(buffer)
        } catch (error: Throwable) {
            frameInFlight.set(false)
            listener.onError("تعذّرت قراءة صورة الكاميرا: ${error.message ?: error.javaClass.simpleName}")
            imageProxy.close()
            return
        }
        imageProxy.close()

        // Do not allocate a second rotated/mirrored bitmap. MediaPipe can apply
        // the camera rotation internally, and front-camera mirroring is applied
        // only to the output coordinates. This removes a full-frame transform
        // and a large temporary allocation from every inference.
        pendingWidth = if (rotation % 180 == 0) width else height
        pendingHeight = if (rotation % 180 == 0) height else width
        pendingMirrorX = frontCamera

        val mpImage = BitmapImageBuilder(bitmap).build()
        val processing = ImageProcessingOptions.builder()
            .setRotationDegrees(rotation)
            .build()
        val timestamp = SystemClock.uptimeMillis()
        try {
            detector.detectAsync(mpImage, processing, timestamp)
        } catch (error: Throwable) {
            frameInFlight.set(false)
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
        val bundle = ResultBundle(
            result = result,
            inputWidth = pendingWidth,
            inputHeight = pendingHeight,
            inferenceMs = SystemClock.uptimeMillis() - result.timestampMs(),
            mirrorX = pendingMirrorX
        )
        listener.onResults(bundle)
        // The backing bitmap may be reused only after MediaPipe has returned
        // this result. Releasing the gate here prevents inference from reading
        // pixels while CameraX overwrites the same bitmap.
        frameInFlight.set(false)
    }

    fun close() {
        frameInFlight.set(false)
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
        val mirrorX: Boolean
    )

    interface Listener {
        fun onReady(delegateName: String)
        fun onResults(bundle: ResultBundle)
        fun onError(message: String)
    }

    companion object {
        private const val TAG = "HandGestureCube"
        private const val MODEL_PATH = "hand_landmarker.task"

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
