package com.example.visionv2.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.visionv2.data.ModelOutput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.scale
import com.example.visionv2.model.ObjectDetectorModel
import com.example.visionv2.tts.TTSHelper

class FrameAnalyzer(
    private val context: Context,
    private val detector: ObjectDetectorModel,
    private val depth: Depth,
    private val tts: TTSHelper,
    private val screenWidth: Float,
    private val screenHeight: Float,
    private val onResults: (List<ModelOutput>) -> Unit,
) : ImageAnalysis.Analyzer {

    @Volatile
    private var currentDetector: ObjectDetectorModel = detector
    private var frameSkipCount = 0
    private val frameSkipInterval = 3

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun analyze(image: ImageProxy) {
        Log.d("ImageProxy", "Image Proxy Size: ${image.width} x ${image.height}")

        if (frameSkipCount % frameSkipInterval == 0) {

            scope.launch {
                Log.d(
                    "FrameAnalyzer",
                    "Image rotation before rotation: ${image.imageInfo.rotationDegrees}"
                )

                val bitmap = image.toBitmap()
                val rotatedBitmap = rotateBitmap(bitmap)
                val resizedBitmap =
                    rotatedBitmap.scale(screenWidth.toInt(), screenHeight.toInt())

                val activeDetector = currentDetector
                val results = withContext(Dispatchers.Default) {
                    val detections = activeDetector.detect(resizedBitmap)
                    depth.depth(resizedBitmap, detections)
                    detections
                }

                if (results.isNotEmpty()) {
                    sortModelOutputsByDistance(results)
                    val distance = results[0].distance.value ?: 0f

                    tts.speakDetection(results[0].classId, distance)
                }

                onResults(results)
                image.close()
            }
        } else {
            image.close()
        }

        frameSkipCount++
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float = 90f): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun sortModelOutputsByDistance(outputs: List<ModelOutput>): List<ModelOutput> {
        val sortedList = outputs.sortedWith(
            compareByDescending<ModelOutput> { it.distance.value == null }
                .thenBy { it.distance.value }
        )

        return sortedList
    }
}