package com.example.visionv2.domain

import android.content.Context
import android.graphics.Bitmap
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
        val modelFile: MappedByteBuffer = FileUtil.loadMappedFile(context, "best-midas.tflite")
        interpreter = Interpreter(modelFile)
        inputShape = interpreter.getInputTensor(0).shape()
    }

    override fun depth(bitmap: Bitmap, modelOutput: List<ModelOutput>) {
        preprocessResult = preprocessBitmap(bitmap, 256)

        val depthOutput = Array(1) { Array(256) { FloatArray(256) } }

        interpreter.run(preprocessResult.inputBuffer, depthOutput)

        assignDepthToObjects(
            modelOutput,
            depthOutput,
            preprocessResult.xOffset,
            preprocessResult.yOffset,
            preprocessResult.scale,
        )
    }

    private fun assignDepthToObjects(
        outputs: List<ModelOutput>,
        depthMap: Array<Array<FloatArray>>, // [1][256][256]
        xOffset: Int,
        yOffset: Int,
        scale: Float,
        targetSize: Int = 256
    ) {
        for (output in outputs) {
            val scaledX = (output.centerX * scale + xOffset).toInt()
            val scaledY = (output.centerY * scale + yOffset).toInt()
            val scaledW = (output.width * scale).toInt()
            val scaledH = (output.height * scale).toInt()

            val left = (scaledX - scaledW / 2).coerceIn(0, targetSize - 1)
            val top = (scaledY - scaledH / 2).coerceIn(0, targetSize - 1)
            val right = (scaledX + scaledW / 2).coerceIn(0, targetSize - 1)
            val bottom = (scaledY + scaledH / 2).coerceIn(0, targetSize - 1)

            val depthRegion = mutableListOf<Float>()
            for (y in top until bottom) {
                for (x in left until right) {
                    depthRegion.add(depthMap[0][y][x])
                }
            }

            if (depthRegion.isNotEmpty()) {
                val medianDepth = depthRegion.sorted()[depthRegion.size / 2]
                output.distance.value = medianDepth
            }
        }
    }
}