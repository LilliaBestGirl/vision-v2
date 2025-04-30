package com.example.visionv2.domain

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.visionv2.data.ModelOutput
import com.example.visionv2.data.PreprocessResult
import com.example.visionv2.utils.preprocessBitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.MappedByteBuffer

class DepthEstimation(
    private val context: Context
): Depth {
    private var interpreter: Interpreter
    private val inputShape: IntArray
    private lateinit var preprocessResult: PreprocessResult

    init {
        val modelFile: MappedByteBuffer = FileUtil.loadMappedFile(context, "midas.tflite")
        interpreter = Interpreter(modelFile)
        inputShape = interpreter.getInputTensor(0).shape()
    }

    override fun depth(bitmap: Bitmap, modelOutput: List<ModelOutput>) {
        preprocessResult = preprocessBitmap(bitmap, 256)

        val outputBuffer = Array(1) { Array(256) { Array(256) { FloatArray(1) } } }

        interpreter.run(preprocessResult.inputBuffer, outputBuffer)

        assignDepthToObjects(
            modelOutput,
            outputBuffer,
            preprocessResult.xOffset,
            preprocessResult.yOffset,
            preprocessResult.scale,
        )
    }

    private fun assignDepthToObjects(
        outputs: List<ModelOutput>,
        depthMap: Array<Array<Array<FloatArray>>>, // [1][256][256]
        xOffset: Int,
        yOffset: Int,
        scale: Float,
        targetSize: Int = 256
    ) {
        for ((index, output) in outputs.withIndex()) {
            val scaledX = (output.centerX * scale + xOffset).toInt()
            val scaledY = (output.centerY * scale + yOffset).toInt()
            val scaledW = (output.width * scale).toInt()
            val scaledH = (output.height * scale).toInt()

            Log.d("DepthAssign", "[$index] Screen center: (${output.centerX}, ${output.centerY})")
            Log.d("DepthAssign", "[$index] Scaled center: ($scaledX, $scaledY), w: $scaledW, h: $scaledH")

            val left = (scaledX - scaledW / 2).coerceIn(0, targetSize - 1)
            val top = (scaledY - scaledH / 2).coerceIn(0, targetSize - 1)
            val right = (scaledX + scaledW / 2).coerceIn(0, targetSize - 1)
            val bottom = (scaledY + scaledH / 2).coerceIn(0, targetSize - 1)

            Log.d("DepthAssign", "[$index] Cropping box: left=$left, top=$top, right=$right, bottom=$bottom")

            val depthRegion = mutableListOf<Float>()
            for (y in top until bottom) {
                for (x in left until right) {
                    depthRegion.add(depthMap[0][y][x][0])
                }
            }

            Log.d("DepthAssign", "[$index] Region size: ${depthRegion.size}")

            if (depthRegion.isNotEmpty()) {
                val sorted = depthRegion.sorted()
                val medianDepth = sorted[sorted.size / 2]
                output.distance.value = medianDepth

                Log.d("DepthAssign", "[$index] Median depth: $medianDepth")
            } else {
                Log.d("DepthAssign", "[$index] No valid region found.")
            }
        }
    }
}