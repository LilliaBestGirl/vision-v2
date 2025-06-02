package com.example.visionv2.model

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.visionv2.data.ModelOutput
import com.example.visionv2.data.PreprocessResult
import com.example.visionv2.domain.Detector
import com.example.visionv2.utils.preprocessBitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.MappedByteBuffer

class ObjectDetectorModel(
    private val context: Context
) : Detector {

    private var interpreter: Interpreter
    private val inputShape: IntArray
    private lateinit var preprocessResult: PreprocessResult

    private var modelName = "best-fp16.tflite"

    private var labelsFile = "outdoor_classes.txt"

    init {
        val modelFile: MappedByteBuffer = FileUtil.loadMappedFile(context, modelName)
        interpreter = Interpreter(modelFile)
        inputShape = interpreter.getInputTensor(0).shape()
    }

    override fun detect(bitmap: Bitmap): List<ModelOutput> {

        preprocessResult = preprocessBitmap(bitmap, 640)
        val outputBuffer = Array(1) { Array(25200) { FloatArray(21) } }

        try {
            interpreter.runForMultipleInputsOutputs(
                arrayOf(preprocessResult.inputBuffer),
                mapOf(0 to outputBuffer)
            )
        } catch(e: Exception) {
            Log.e("Interpreter", "Interpreter Error: ${e.message}")
        }

        val rawResults = parseResults(outputBuffer)

        return nonMaxSuppression(rawResults)
    }

    private val labelMap: Map<String, String> = loadLabels(context, labelsFile)

    private fun loadLabels(context: Context, fileName: String): Map<String, String> {
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

    private val idMap =
        listOf(
            "/m/0130jx", // Sink
            "/m/0199g", // Bicycle
            "/m/01bjv", // Bus
            "/m/01g317", // Person
            "/m/01mzpv", // Chair
            "/m/02crq1", // Couch
            "/m/02dgv", // Door
            "/m/03ssj5", // Bed
            "/m/040b_t", // Refrigerator
            "/m/04_sv", // Motorcycle
            "/m/04bcr3", // Table
            "/m/07c52", // Television
            "/m/07r04", // Truck
            "/m/09g1w", // Toilet
            "/m/0cvnqh", // Bench
            "/m/0k4j", // Car
        )

    private fun parseResults(
        outputBuffer: Array<Array<FloatArray>>
    ): List<ModelOutput> {
        val results = mutableListOf<ModelOutput>()

        for (i in 0 until 25200) {
            val box = outputBuffer[0][i]

            val objectness = box[4]
            if (objectness < 0.3) continue

            val centerX = (box[0] * 640 - preprocessResult.xOffset) / preprocessResult.scale
            val centerY = (box[1] * 640 - preprocessResult.yOffset) / preprocessResult.scale
            val width = (box[2] * 640) / preprocessResult.scale
            val height = (box[3] * 640) / preprocessResult.scale

            val classScores = box.sliceArray(5 until 21)
            val maxClassIndex = classScores.indices.maxByOrNull { classScores[it] } ?: -1
            Log.d("ClassIndex", "Class Index: $maxClassIndex")

            val classId = idMap.getOrNull(maxClassIndex) ?: "Unknown"
            val label = labelMap[classId] ?: "Unknown"

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