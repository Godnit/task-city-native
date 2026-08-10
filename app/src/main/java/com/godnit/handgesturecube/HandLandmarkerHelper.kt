/*
 * Restored v1.3-era geometry with low-latency frame handling.
 * The camera frame is physically rotated + mirrored before MediaPipe exactly
 * like the old working builds, so landmarks can be drawn without any extra
 * coordinate transform.
 */
package com.godnit.handgesturecube

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.util.concurrent.atomic.AtomicBoolean

class HandLandmarkerHelper(
    private val context: Context,
    private val listener: Listener
) {
    private var handLandmarker: HandLandmarker? = null
    private var sourceBitmap: Bitmap? = null
    private var pendingOrientedBitmap: Bitmap? = null
    private var pendingMpImage: MPImage? = null
    private val frameInFlight = AtomicBoolean(false)
    private var resultCount = 0
    private var delegateName = "CPU"

    fun setup() {
        try {
            handLandmarker = try {
                createLandmarker(Delegate.GPU).also { delegateName = "GPU" }
            } catch (gpuError: Throwable) {
                Log.w(TAG, "GPU unavailable; using CPU", gpuError)
                createLandmarker(Delegate.CPU).also { delegateName = "CPU" }
            }
            Log.i(TAG, "HAND_TRACKER_READY_$delegateName")
            listener.onReady(delegateName)
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
            // Once a hand is found, prefer MediaPipe's tracking path instead
            // of repeatedly paying for a fresh palm detection.
            .setMinHandPresenceConfidence(0.25f)
            .setMinTrackingConfidence(0.18f)
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

        // Critical latency rule: never queue an old frame behind an inference.
        if (!frameInFlight.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val width = imageProxy.width
        val height = imageProxy.height
        val rotation = imageProxy.imageInfo.rotationDegrees
        val bitmap = obtainSourceBitmap(width, height)

        try {
            val buffer = imageProxy.planes[0].buffer
            buffer.rewind()
            bitmap.copyPixelsFromBuffer(buffer)
        } catch (error: Throwable) {
            imageProxy.close()
            frameInFlight.set(false)
            listener.onError("تعذّرت قراءة صورة الكاميرا: ${error.message ?: error.javaClass.simpleName}")
            return
        }
        imageProxy.close()

        // Keep this transformation intentionally identical to the old v1.3
        // family. This is what made the skeleton sit directly on the hand.
        val matrix = Matrix().apply {
            postRotate(rotation.toFloat())
            if (frontCamera) {
                postScale(-1f, 1f, width.toFloat(), height.toFloat())
            }
        }

        val oriented = try {
            Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false)
        } catch (error: Throwable) {
            frameInFlight.set(false)
            listener.onError("تعذّر تجهيز اتجاه صورة الكاميرا: ${error.message ?: error.javaClass.simpleName}")
            return
        }
        pendingOrientedBitmap = oriented

        val mpImage = try {
            BitmapImageBuilder(oriented).build()
        } catch (error: Throwable) {
            oriented.recycle()
            pendingOrientedBitmap = null
            frameInFlight.set(false)
            listener.onError("تعذّر تجهيز إطار التتبع: ${error.message ?: error.javaClass.simpleName}")
            return
        }
        pendingMpImage = mpImage

        try {
            detector.detectAsync(mpImage, SystemClock.uptimeMillis())
        } catch (error: Throwable) {
            releaseInFlight()
            listener.onError("تعذّر تحليل صورة الكاميرا: ${error.message ?: error.javaClass.simpleName}")
        }
    }

    private fun obtainSourceBitmap(width: Int, height: Int): Bitmap {
        val current = sourceBitmap
        if (current == null || current.width != width || current.height != height || current.isRecycled) {
            current?.recycle()
            sourceBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
        return sourceBitmap!!
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
                    inputWidth = input.width,
                    inputHeight = input.height,
                    inferenceMs = SystemClock.uptimeMillis() - result.timestampMs(),
                    delegateName = delegateName
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
            pendingOrientedBitmap?.recycle()
        } catch (_: Throwable) {
        }
        pendingOrientedBitmap = null
        frameInFlight.set(false)
    }

    fun close() {
        releaseInFlight()
        handLandmarker?.close()
        handLandmarker = null
        sourceBitmap?.recycle()
        sourceBitmap = null
    }

    data class ResultBundle(
        val result: HandLandmarkerResult,
        val inputWidth: Int,
        val inputHeight: Int,
        val inferenceMs: Long,
        val delegateName: String
    )

    interface Listener {
        fun onReady(delegateName: String)
        fun onResults(bundle: ResultBundle)
        fun onError(message: String)
    }

    companion object {
        private const val TAG = "HandGestureCube"
        private const val MODEL_PATH = "hand_landmarker.task"
    }
}
