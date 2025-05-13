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
    private val context: Context,
    val isIndoorMode: Boolean
) : Detector {

    private var interpreter: Interpreter
    private val inputShape: IntArray
    private lateinit var preprocessResult: PreprocessResult

    // will be used once indoor model has been received
    private var modelName = if (isIndoorMode) "yolov5_indoor_model.tflite" else "yolov5_outdoor_model.tflite"

    private var labelsFile = if (isIndoorMode) "indoor_classes.txt" else "outdoor_classes.txt"

    init {
        // Load TFLite model
        val modelFile: MappedByteBuffer = FileUtil.loadMappedFile(context, modelName)
        interpreter = Interpreter(modelFile)
        inputShape = interpreter.getInputTensor(0).shape()
    }

    override fun detect(bitmap: Bitmap): List<ModelOutput> {
        if (isIndoorMode) {
            Log.d("Indoor", "Is Indoor Mode: $isIndoorMode")
        } else {
            Log.d("Outdoor", "Is Indoor Mode: $isIndoorMode")
        }

        // Resize and normalize the input
        preprocessResult = preprocessBitmap(bitmap, 640)

        // Output array
        val outputBuffer = if (isIndoorMode) {
            Array(1) { Array(25200) { FloatArray(14) } }
        } else {
            Array(1) { Array(25200) { FloatArray(12) } }
        }


        // Run inference
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

    private val idMap = if (!isIndoorMode) {
        listOf(
            "/m/0199g",  // bicycle
            "/m/01bjv",  // bus
            "/m/01g317", // person
            "/m/04_sv",  // motorcycle
            "/m/07r04",   // truck
            "/m/0cvnqh", // bench
            "/m/0k4j",   // car
        )
    } else {
        listOf(
            "/m/0130jx", // Sink
            "/m/01mzpv", // Chair
            "/m/02crq1", // Couch
            "/m/02dgv", // Door
            "/m/03ssj5", // Bed
            "/m/040b_t", // Refrigerator
            "/m/04bcr3", // Table
            "/m/07c52", // Television
            "/m/09g1w", // Toilet
        )
    }

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

            val classScores = box.sliceArray(5 until 12)
            val maxClassIndex = classScores.indices.maxByOrNull { classScores[it] } ?: -1

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

        // Sort by confidence and take top 5 predictions
        return results.sortedByDescending { it.score }.take(5)
    }

    override fun close() {
        interpreter.close()
        Log.d("Interpreter", "Interpreter closed")
    }

}