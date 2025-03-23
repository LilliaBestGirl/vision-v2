package com.example.visionv2.domain

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.visionv2.data.ModelOutput
import com.example.visionv2.model.ObjectDetectorModel

class FrameAnalyzer(
    private val onResults: (List<ModelOutput>) -> Unit,
    private val detector: ObjectDetectorModel
) : ImageAnalysis.Analyzer {

    private var frameSkipCount = 0
    private val frameSkipInterval = 3

    override fun analyze(image: ImageProxy) {
        Log.d("ImageProxy", "Image Proxy Size: ${image.width} x ${image.height}")

        if (frameSkipCount % frameSkipInterval == 0) {
            Log.d("FrameAnalyzer", "Analyzing frame...")
            val bitmap = image.toBitmap()
            if (bitmap != null) {
                val rotatedBitmap = rotateBitmap(bitmap, 90f)

                val results = detector.detect(rotatedBitmap)

                onResults(results)
            }
        }

        frameSkipCount++

        image.close()
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}