package com.example.visionv2.model

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.visionv2.data.ModelOutput
import com.example.visionv2.data.PreprocessResult
import com.example.visionv2.domain.Detector
import com.example.visionv2.utils.preprocessBitmapYolo
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.MappedByteBuffer

class ObjectDetectorModel(
    private val context: Context
) : Detector {

    private var interpreter: Interpreter
    private val inputShape: IntArray
    private lateinit var preprocessResult: PreprocessResult

    private var modelName = "best-fp16-new.tflite"

    init {
        val modelFile: MappedByteBuffer = FileUtil.loadMappedFile(context, modelName)
        interpreter = Interpreter(modelFile)
        inputShape = interpreter.getInputTensor(0).shape()
    }

    override fun detect(bitmap: Bitmap): List<ModelOutput> {

        preprocessResult = preprocessBitmapYolo(bitmap, 640)
        val inputBuffer = preprocessResult.inputBuffer
        val outputBuffer = Array(1) { Array(25200) { FloatArray(24) } }

        try {
            interpreter.runForMultipleInputsOutputs(
                arrayOf(inputBuffer),
                mapOf(0 to outputBuffer)
            )
        } catch (e: Exception) {
            Log.e("Interpreter", "Interpreter Error: ${e.message}")
        }

        val rawResults = parseResults(outputBuffer)

        return nonMaxSuppression(rawResults)
    }

    private val labelMap: List<String> =
        listOf(
            "Sink",
            "Traffic light",
            "Bicycle",
            "Bus",
            "Person",
            "Chair",
            "Couch",
            "Door",
            "Street light",
            "Bed",
            "Refrigerator",
            "Motorcycle",
            "Table",
            "Television",
            "Truck",
            "Toilet",
            "Bench",
            "Car",
            "Stairs"
        )

    private fun parseResults(
        outputBuffer: Array<Array<FloatArray>>
    ): List<ModelOutput> {
        val results = mutableListOf<ModelOutput>()
        val confidenceThreshold = 0.5

        for (i in 0 until 25200) {
            val box = outputBuffer[0][i]

            val objectness = box[4]
            if (objectness < confidenceThreshold) continue

            val centerX = (box[0] * 640 - preprocessResult.xOffset) / preprocessResult.scale
            val centerY = (box[1] * 640 - preprocessResult.yOffset) / preprocessResult.scale
            val width = (box[2] * 640) / preprocessResult.scale
            val height = (box[3] * 640) / preprocessResult.scale

            val classScores = box.sliceArray(5 until box.size)
            val maxClassIndex = classScores.indices.maxByOrNull { classScores[it] } ?: -1

            val label = labelMap[maxClassIndex]

            results.add(
                ModelOutput(
                    centerX = centerX,
                    centerY = centerY,
                    width = width,
                    height = height,
                    score = objectness,
                    classId = maxClassIndex,
                    name = label
                )
            )
        }

        return results.sortedByDescending { it.score }.take(5)
    }

    override fun close() {
        interpreter.close()
        Log.d("Interpreter", "Interpreter closed")
    }

}