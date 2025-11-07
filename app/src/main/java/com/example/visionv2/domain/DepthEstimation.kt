package com.example.visionv2.domain

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.visionv2.data.ModelOutput
import com.example.visionv2.data.PreprocessResult
import com.example.visionv2.utils.preprocessBitmapMidas
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.MappedByteBuffer

class DepthEstimation(
    private val context: Context
): Depth {
    private var interpreter: Interpreter
    private val inputShape: IntArray

    private lateinit var preprocessResult: PreprocessResult
    private lateinit var outputBuffer: Array<Array<Array<FloatArray>>>

    init {
        val modelFile: MappedByteBuffer = FileUtil.loadMappedFile(context, "midas.tflite")
        interpreter = Interpreter(modelFile)
        inputShape = interpreter.getInputTensor(0).shape()
    }

    override fun depth(bitmap: Bitmap, modelOutput: List<ModelOutput>) {
        preprocessResult = preprocessBitmapMidas(bitmap, 256)
        val inputBuffer = preprocessResult.inputBuffer

        outputBuffer = Array(1) { Array(256) { Array(256) { FloatArray(1) } } }

        interpreter.run(inputBuffer, outputBuffer)

        assignDepthToObjects(
            modelOutput,
            outputBuffer
        )
    }

    private fun assignDepthToObjects(
        outputs: List<ModelOutput>,
        depthMap: Array<Array<Array<FloatArray>>>, // [1][256][256]
    ) {
        val midasFrameSize = 256

        val regionSize = 5
        val half = regionSize / 2

        for ((index, output) in outputs.withIndex()) {
            val xOrig = output.centerX
            val yOrig = output.centerY
            val xOffset = preprocessResult.xOffset
            val yOffset = preprocessResult.yOffset
            val scale = preprocessResult.scale

            val flatDepthValues = depthMap[0].flatMap { row ->
                row.map { it[0] }
            }

            val minVal = flatDepthValues.minOrNull() ?: 0f
            val maxVal = flatDepthValues.maxOrNull() ?: 1f

            val depthX = (xOrig * scale + xOffset).toInt().coerceIn(0, midasFrameSize - 1)
            val depthY = (yOrig * scale + yOffset).toInt().coerceIn(0, midasFrameSize - 1)

            Log.d("DepthAssign", "[$index] Original center: (${output.centerX}, ${output.centerY})")
            Log.d("DepthAssign", "[$index] Scaled center: ($depthX, $depthY)")

            val depthValues = mutableListOf<Float>()

            for (dy in -half..half) {
                for (dx in -half..half) {
                    val x = (depthX + dx).coerceIn(0, 255)
                    val y = (depthY + dy).coerceIn(0, 255)
                    val depth = depthMap[0][y][x][0]

                    val normalizedDepth = ((maxVal - depth) / (maxVal - minVal)).coerceIn(0f, 1f)
                    depthValues.add(normalizedDepth)
                }
            }

            val median = depthValues.sorted()[depthValues.size / 2]

            Log.d("DepthAssign", "[$index] median depth: $median")

            output.distance.value = median
        }
    }
}