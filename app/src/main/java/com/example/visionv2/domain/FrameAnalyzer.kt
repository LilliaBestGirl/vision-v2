package com.example.visionv2.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.visionv2.data.ModelOutput
import com.example.visionv2.model.ObjectDetectorModel
import com.google.ar.core.Session
import java.io.ByteArrayOutputStream
import kotlin.math.min

class FrameAnalyzer(
    private val context: Context,
    private val onResults: (List<ModelOutput>) -> Unit,
    private val screenWidth: Float,
    private val screenHeight: Float,
    private val detector: ObjectDetectorModel
) : ImageAnalysis.Analyzer {

    private val ttsHelper = TTSHelper(context)
    private var frameSkipCount = 0
    private val frameSkipInterval = 3

    override fun analyze(image: ImageProxy) {
        Log.d("ImageProxy", "Image Proxy Size: ${image.width} x ${image.height}")

        if (frameSkipCount % frameSkipInterval == 0) {
            Log.d("FrameAnalyzer", "Image rotation before rotation: ${image.imageInfo.rotationDegrees}")

            val bitmap = image.toBitmap()
            if (bitmap != null) {
                val rotatedBitmap = rotateBitmap(bitmap)
                val resizedBitmap = Bitmap.createScaledBitmap(rotatedBitmap, screenWidth.toInt(), screenHeight.toInt(), true)

                val results = detector.detect(resizedBitmap)

                if (results.isNotEmpty()) {
                    val distance = results[0].distance.value ?: 0f
                    val spokenDistance = if (distance > 0f) {
                        "$distance meters away"
                    } else {
                        "Distance Unknown"
                    }

                    ttsHelper.speak("${results[0].name} detected, $spokenDistance")
                    Log.d("TTSHelper", "TTS says: ${"%.1f".format(distance)}")
                }

                onResults(results)
            }
        }

        frameSkipCount++

        image.close()
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float = 90f): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

}