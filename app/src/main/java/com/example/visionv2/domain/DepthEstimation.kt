package com.example.visionv2.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.example.visionv2.data.ModelOutput
import com.example.visionv2.data.PreprocessResult
import com.example.visionv2.utils.preprocessBitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.MappedByteBuffer
import androidx.core.graphics.createBitmap

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
        val flatDepth = mutableListOf<Float>()
        for (y in 0 until targetSize) {
            for (x in 0 until targetSize) {
                flatDepth.add(depthMap[0][y][x][0])
            }
        }

        val minDepth = flatDepth.minOrNull() ?: 0f
        val maxDepth = flatDepth.maxOrNull() ?: 1f
        val range = (maxDepth - minDepth).takeIf { it > 0f } ?: 1f

        for ((index, output) in outputs.withIndex()) {
            // Scale the bounding box
            val scaledX = (output.centerX * scale + xOffset).toInt()
            val scaledY = (output.centerY * scale + yOffset).toInt()

            // Log scaled values for debugging
            Log.d("DepthAssign", "[$index] Scaled center: ($scaledX, $scaledY)")

            // Calculate the center coordinates of the bounding box in depth map coordinates
            val centerX = (scaledX * targetSize / 256).coerceIn(0, targetSize - 1)
            val centerY = (scaledY * targetSize / 256).coerceIn(0, targetSize - 1)

            // Get the depth value at the center of the bounding box
            val depthValue = depthMap[0][centerY][centerX][0]

            // Normalize the depth value
            val normalizedDepth = (depthValue - minDepth) / range

            // Classify depth range
            val label = when {
                normalizedDepth >= 0.75f -> "Very close"
                normalizedDepth >= 0.4f -> "Close"
                else -> "Far away"
            }

            // Assign the distance and label to the output object
            output.distance.value = normalizedDepth
            output.distanceLabel.value = label

            // Log the results
            Log.d("DepthAssign", "[$index] Depth at center: $depthValue | Normalized: $normalizedDepth | Label: $label")

//            val scaledX = (output.centerX * scale + xOffset).toInt()
//            val scaledY = (output.centerY * scale + yOffset).toInt()
//            val scaledW = (output.width * scale).toInt()
//            val scaledH = (output.height * scale).toInt()
//
//            Log.d("DepthAssign", "[$index] Screen center: (${output.centerX}, ${output.centerY})")
//            Log.d("DepthAssign", "[$index] Scaled center: ($scaledX, $scaledY), w: $scaledW, h: $scaledH")
//
//            val left = (scaledX - scaledW / 2).coerceIn(0, targetSize - 1)
//            val top = (scaledY - scaledH / 2).coerceIn(0, targetSize - 1)
//            val right = (scaledX + scaledW / 2).coerceIn(0, targetSize - 1)
//            val bottom = (scaledY + scaledH / 2).coerceIn(0, targetSize - 1)
//
//            Log.d("DepthAssign", "[$index] Cropping box: left=$left, top=$top, right=$right, bottom=$bottom")
//
//            var sumDepth = 0f
//            var count = 0
//
//            for (y in top until bottom) {
//                for (x in left until right) {
//                    val rawDepth = depthMap[0][y][x][0]
//                    sumDepth += rawDepth
//                    count++
//                }
//            }
//
//            if (count > 0) {
//                val avgDepth = sumDepth / count
//                val normalizedDepth = (avgDepth - minDepth) / range
//
//                // Map to label
//                val label = when {
//                    normalizedDepth >= 0.75f -> "Very close"
//                    normalizedDepth >= 0.4f -> "Close"
//                    else -> "Far away"
//                }
//
//                output.distance.value = normalizedDepth
//                output.distanceLabel.value = label
//
//                Log.d("DepthAssign", "[$index] Avg depth: $avgDepth | Normalized: $normalizedDepth | Label: $label")
//            } else {
//                Log.d("DepthAssign", "[$index] No valid region found.")
//                output.distance.value = -1f
//            }
        }
    }
}