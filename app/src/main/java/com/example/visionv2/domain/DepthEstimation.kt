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
    private lateinit var outputBuffer: Array<Array<Array<FloatArray>>>

    init {
        val modelFile: MappedByteBuffer = FileUtil.loadMappedFile(context, "midas.tflite")
        interpreter = Interpreter(modelFile)
        inputShape = interpreter.getInputTensor(0).shape()
    }

    override fun depth(bitmap: Bitmap, modelOutput: List<ModelOutput>) {
        preprocessResult = preprocessBitmap(bitmap, 256)

        outputBuffer = Array(1) { Array(256) { Array(256) { FloatArray(1) } } }

        interpreter.run(preprocessResult.inputBuffer, outputBuffer)
        Log.d("Depth", "${outputBuffer[0]}")

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

            Log.d("DepthAssign", "[$index] Scaled center: ($scaledX, $scaledY)")

            val centerX = (scaledX * targetSize / 256).coerceIn(0, targetSize - 1)
            val centerY = (scaledY * targetSize / 256).coerceIn(0, targetSize - 1)

            val depthValue = depthMap[0][centerY][centerX][0]
            val label = when {
                depthValue >= 800 -> "less than 1 meter away"
                depthValue >= 500 -> "1.5 to 3 meters away"
                depthValue >= 300 -> "3.5 to 4 meters away"
                else -> "5m away"
            }

            output.distance.value = depthValue
            output.distanceLabel.value = label

            Log.d("DepthAssign", "[$index] Depth at center: $depthValue | Label: $label")
        }
    }
}