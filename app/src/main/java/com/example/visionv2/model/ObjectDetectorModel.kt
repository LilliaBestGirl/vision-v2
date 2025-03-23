package com.example.visionv2.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.example.visionv2.data.ModelOutput
import com.example.visionv2.domain.Detector
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import kotlin.math.floor
import kotlin.math.min

class ObjectDetectorModel(
    private val context: Context
) : Detector {

    private var interpreter: Interpreter
    private val inputShape: IntArray

    init {
        // Load TFLite model
        val modelFile: MappedByteBuffer = FileUtil.loadMappedFile(context, "best-fp16.tflite")
        interpreter = Interpreter(modelFile)
        inputShape = interpreter.getInputTensor(0).shape()
    }

    override fun detect(bitmap: Bitmap): List<ModelOutput> {
        // Resize and normalize the input
        val inputBuffer = preprocessBitmap(bitmap)

        // Output array
        val outputBuffer = Array(1) { Array(25200) { FloatArray(12) } }

        // Run inference
        interpreter.runForMultipleInputsOutputs(
            arrayOf(inputBuffer),
            mapOf(0 to outputBuffer)
        )


        return parseResults(outputBuffer)
    }

    private val labelMap: Map<String, String> = loadLabels(context)

    private fun loadLabels(context: Context, fileName: String = "classes.txt"): Map<String, String> {
        val labelMap = mutableMapOf<String, String>()
        context.assets.open(fileName).bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val parts = line.split(" ")
                if (parts.size == 2) {
                    val id = parts[0]
                    val label = parts[1]
                    labelMap[id] = label
                }
            }
        }
        return labelMap
    }

    private val idMap = listOf(
        "/m/0199g",  // bicycle
        "/m/01bjv",  // bus
        "/m/01g317", // person
        "/m/04_sv",  // motorcycle
        "/m/07r04",   // truck
        "/m/0cvnqh", // bench
        "/m/0k4j",   // car
    )

    private var xOffset = 0
    private var yOffset = 0
    private var scale = 0f
    private val targetSize = 640

    @Suppress("DEPRECATION")
    private fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        Log.d("Preprocess", "Original Size: $originalWidth x $originalHeight")

        // Scale while preserving aspect ratio
        scale = min(targetSize.toFloat() / originalWidth, targetSize.toFloat() / originalHeight)
        val newWidth = (originalWidth * scale).toInt()
        val newHeight = (originalHeight * scale).toInt()

        Log.d("Preprocess", "New Size: $newWidth x $newHeight")

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

        // Calculate padding for letterboxing
        xOffset = floor((targetSize - newWidth) / 2f).toInt()
        yOffset = floor((targetSize - newHeight) / 2f).toInt()

        val paddedBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(paddedBitmap)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        canvas.drawColor(Color.BLACK) // Fill background with black
        canvas.drawBitmap(resizedBitmap, xOffset.toFloat(), yOffset.toFloat(), paint)

        val inputBuffer = ByteBuffer.allocateDirect(targetSize * targetSize * 3 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(targetSize * targetSize)
        paddedBitmap.getPixels(intValues, 0, targetSize, 0, 0, targetSize, targetSize)

        for (pixel in intValues) {
            val r = (pixel shr 16 and 0xFF) / 255.0f
            val g = (pixel shr 8 and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            inputBuffer.putFloat(r)
            inputBuffer.putFloat(g)
            inputBuffer.putFloat(b)
        }

        return inputBuffer
    }


    private fun parseResults(
        outputBuffer: Array<Array<FloatArray>>
    ): List<ModelOutput> {
        val results = mutableListOf<ModelOutput>()

        for (i in 0 until 25200) { // 25200 grid cells
            val box = outputBuffer[0][i]

            val objectness = box[4]
            if (objectness < 0.3) continue

            val centerX = (box[0] * 640 - xOffset) / scale
            val centerY = (box[1] * 640 - yOffset) / scale
            val width = (box[2] * 640) / scale
            val height = (box[3] * 640) / scale

            val left = centerX - width / 2
            val top = centerY - height / 2
            val right = centerX + width / 2
            val bottom = centerY + height / 2

            // Get class scores (5 to 12 because we have 7 classes)
            val classScores = box.sliceArray(5 until 12)
            val maxClassIndex = classScores.indices.maxByOrNull { classScores[it] } ?: -1
//            val maxClassScore = if (maxClassIndex != -1) classScores[maxClassIndex] else 0f

            val classId = idMap.getOrNull(maxClassIndex) ?: "Unknown"
            val label = labelMap[classId] ?: "Unknown"

            results.add(
                ModelOutput(
                    left = left,
                    top = top,
                    right = right,
                    bottom = bottom,
                    score = objectness,
                    classId = maxClassIndex,
                    name = label
                )
            )
        }

        // Sort by confidence and take top 5 predictions
        return results.sortedByDescending { it.score }.take(5)
    }

}