/*
 * Based on the Apache-2.0 MediaPipe Hand Landmarker Android sample.
 * Copyright 2022 The TensorFlow Authors.
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

class HandLandmarkerHelper(
    private val context: Context,
    private val listener: Listener
) {
    private var handLandmarker: HandLandmarker? = null
    private var resultCount = 0

    fun setup() {
        try {
            val baseOptions = BaseOptions.builder()
                .setDelegate(Delegate.CPU)
                .setModelAssetPath(MODEL_PATH)
                .build()

            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumHands(1)
                .setMinHandDetectionConfidence(0.42f)
                .setMinHandPresenceConfidence(0.42f)
                .setMinTrackingConfidence(0.42f)
                .setResultListener(::onResult)
                .setErrorListener { listener.onError(it.message ?: "خطأ غير معروف في متتبع اليد") }
                .build()

            handLandmarker = HandLandmarker.createFromOptions(context, options)
            Log.i(TAG, "HAND_TRACKER_READY")
            listener.onReady()
        } catch (error: Throwable) {
            listener.onError("تعذّر تحميل نموذج اليد: ${error.message ?: error.javaClass.simpleName}")
        }
    }

    fun detect(imageProxy: ImageProxy, frontCamera: Boolean) {
        val detector = handLandmarker
        if (detector == null) {
            imageProxy.close()
            return
        }

        val width = imageProxy.width
        val height = imageProxy.height
        val rotation = imageProxy.imageInfo.rotationDegrees
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            bitmap.copyPixelsFromBuffer(imageProxy.planes[0].buffer)
        } catch (error: Throwable) {
            listener.onError("تعذّرت قراءة صورة الكاميرا: ${error.message ?: error.javaClass.simpleName}")
            imageProxy.close()
            return
        }
        imageProxy.close()

        val matrix = Matrix().apply {
            postRotate(rotation.toFloat())
            if (frontCamera) {
                postScale(-1f, 1f, width.toFloat(), height.toFloat())
            }
        }
        val rotated = try {
            Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
        } catch (error: Throwable) {
            listener.onError("تعذّر تدوير صورة الكاميرا: ${error.message ?: error.javaClass.simpleName}")
            return
        }
        val mpImage = BitmapImageBuilder(rotated).build()
        try {
            detector.detectAsync(mpImage, SystemClock.uptimeMillis())
        } catch (error: Throwable) {
            listener.onError("تعذّر تحليل صورة الكاميرا: ${error.message ?: error.javaClass.simpleName}")
        }
    }

    private fun onResult(result: HandLandmarkerResult, input: MPImage) {
        resultCount++
        if (resultCount == 1 || resultCount == 10) {
            Log.i(TAG, "HAND_TRACKER_RESULT_$resultCount")
        }
        listener.onResults(
            ResultBundle(
                result = result,
                inputWidth = input.width,
                inputHeight = input.height,
                inferenceMs = SystemClock.uptimeMillis() - result.timestampMs()
            )
        )
    }

    fun close() {
        handLandmarker?.close()
        handLandmarker = null
    }

    data class ResultBundle(
        val result: HandLandmarkerResult,
        val inputWidth: Int,
        val inputHeight: Int,
        val inferenceMs: Long
    )

    interface Listener {
        fun onReady()
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
                    Log.i(TAG, "HAND_TRACKER_SELF_TEST_${index + 1}")
                }
                Log.i(TAG, "HAND_TRACKER_SELF_TEST_PASSED")
            } finally {
                detector.close()
            }
        }
    }
}
