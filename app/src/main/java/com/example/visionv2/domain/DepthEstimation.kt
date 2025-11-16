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

    private val OUTPUT_SCALE = 6.514299392700195f
    private val DEPTH_CALIB_A = 0.012256f
    private val DEPTH_CALIB_B = 0.102924f

    private lateinit var preprocessResult: PreprocessResult
    private lateinit var outputBuffer: Array<Array<Array<ByteArray>>>

    init {
        val modelFile: MappedByteBuffer = FileUtil.loadMappedFile(context, "Midas-V2_w8a8.tflite")
        interpreter = Interpreter(modelFile)
        inputShape = interpreter.getInputTensor(0).shape()
    }

    override fun depth(bitmap: Bitmap, modelOutput: List<ModelOutput>) {
        preprocessResult = preprocessBitmapMidas(bitmap, 256)
        val inputBuffer = preprocessResult.inputBuffer

        outputBuffer = Array(1) { Array(256) { Array(256) { ByteArray(1) } } }

        interpreter.run(inputBuffer, outputBuffer)

        assignDepthToObjects(
            modelOutput,
            outputBuffer
        )
    }

    private fun assignDepthToObjects(
        outputs: List<ModelOutput>,
        depthMap: Array<Array<Array<ByteArray>>>, // [1][256][256][1]
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

            val depthX = (xOrig * scale + xOffset).toInt().coerceIn(0, midasFrameSize - 1)
            val depthY = (yOrig * scale + yOffset).toInt().coerceIn(0, midasFrameSize - 1)

            val depthValues = mutableListOf<Int>()

            for (dy in -half..half) {
                for (dx in -half..half) {
                    val x = (depthX + dx).coerceIn(0, 255)
                    val y = (depthY + dy).coerceIn(0, 255)

                    val rawDepthInt = depthMap[0][y][x][0].toInt() and 0xFF
                    depthValues.add(rawDepthInt)
                }
            }

            val medianQuantized = depthValues.sorted()[depthValues.size / 2].toFloat()

            val relativeDepthValue = OUTPUT_SCALE * medianQuantized

            val inverseDistance = (DEPTH_CALIB_A * relativeDepthValue) + DEPTH_CALIB_B
            val Z_meters: Float

            if (inverseDistance > 0.001f) {
                Z_meters = 1.0f / inverseDistance
            } else {
                Z_meters = 10.0f
            }

//            Log.d("METRIC_DISTANCE", "[$index] Raw Value: $median | Est. Distance: ${"%.2f".format(Z_meters)} meters")

            output.distance.value = Z_meters
        }
    }
}