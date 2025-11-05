package com.example.visionv2.domain

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.visionv2.data.ModelOutput
import com.example.visionv2.utils.preprocessBitmapMidas
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.MappedByteBuffer

class DepthEstimation(
    private val context: Context
): Depth {
    private var interpreter: Interpreter
    private val inputShape: IntArray
    private lateinit var outputBuffer: Array<Array<Array<FloatArray>>>

    init {
        val modelFile: MappedByteBuffer = FileUtil.loadMappedFile(context, "midas.tflite")
        interpreter = Interpreter(modelFile)
        inputShape = interpreter.getInputTensor(0).shape()
    }

    override fun depth(bitmap: Bitmap, modelOutput: List<ModelOutput>) {
        val inputBuffer = preprocessBitmapMidas(bitmap)

        outputBuffer = Array(1) { Array(256) { Array(256) { FloatArray(1) } } }

        interpreter.run(inputBuffer, outputBuffer)
        Log.d("Depth", "${outputBuffer[0]}")

        assignDepthToObjects(
            modelOutput,
            outputBuffer
        )
    }

    private fun assignDepthToObjects(
        outputs: List<ModelOutput>,
        depthMap: Array<Array<Array<FloatArray>>>, // [1][256][256]
    ) {
        val yoloFrameSize = 640f
        val midasFrameSize = 256f
        val scale = midasFrameSize / yoloFrameSize

        val regionSize = 5
        val half = regionSize / 2

        for ((index, output) in outputs.withIndex()) {
            val depthX = (output.centerX * scale).toInt().coerceIn(0, midasFrameSize.toInt() - 1)
            val depthY = (output.centerY * scale).toInt().coerceIn(0, midasFrameSize.toInt() - 1)

            Log.d("DepthAssign", "[$index] Original center: (${output.centerX}, ${output.centerY})")
            Log.d("DepthAssign", "[$index] Scaled center: ($depthX, $depthY)")

            val depthValues = mutableListOf<Float>()

            for (dy in -half..half) {
                for (dx in -half..half) {
                    val x = (depthX + dx).coerceIn(0, 255)
                    val y = (depthY + dy).coerceIn(0, 255)
                    val depth = depthMap[0][y][x][0]
                    depthValues.add(depth)
                }
            }

            val median = depthValues.sorted()[depthValues.size / 2]

            Log.d("DepthAssign", "[$index] median depth: $median")

            output.distance.value = median
        }
    }
}