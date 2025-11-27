package com.example.visionv2.domain

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.visionv2.data.ModelOutput
import com.example.visionv2.data.PreprocessResult
import com.example.visionv2.utils.CalibrationConstants
import com.example.visionv2.utils.CalibrationPresets
import com.example.visionv2.utils.preprocessBitmapMidas
import com.example.visionv2.utils.SimpleKalmanFilter // 🚨 NEW IMPORT
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.MappedByteBuffer

class DepthEstimation(
    private val context: Context
): Depth {
    private var interpreter: Interpreter
    private val inputShape: IntArray

    private val OUTPUT_SCALE = 6.514299392700195f
    private val ZERO_POINT = 0.0f

    // 🚨 NEW PROPERTY: Stores one Kalman filter instance for each detected object ID (index)
    private val kalmanFilters = HashMap<Int, SimpleKalmanFilter>() // Key = Object ID/Index

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

        val constants = determineCalibrationPreset(modelOutput)

        assignDepthToObjects(
            modelOutput,
            outputBuffer,
            constants
        )
    }

    private fun determineCalibrationPreset(
        modelOutputs: List<ModelOutput>
    ): CalibrationConstants {
        val detectedClasses = modelOutputs.map { it.name.toLowerCase() }.toSet()

        // Priority 1: Check for HIGH CONFIDENCE OUTDOOR objects
        if (detectedClasses.any { it in CalibrationPresets.SEMANTIC_OUTDOOR_CLASSES }) {
            Log.d("CALIB_SWITCH", "Found Outdoor object -> Using OUTDOOR_DEFAULT")
            return CalibrationPresets.OUTDOOR_DEFAULT
        }

        // Priority 2: Check for HIGH CONFIDENCE INDOOR objects
        if (detectedClasses.any { it in CalibrationPresets.SEMANTIC_INDOOR_CLASSES }) {
            Log.d("CALIB_SWITCH", "Found Indoor object -> Using INDOOR_DEFAULT")
            return CalibrationPresets.INDOOR_DEFAULT
        }

        // Fallback: Default to OUTDOOR_DEFAULT
        Log.d("CALIB_SWITCH", "No definitive object -> Defaulting to OUTDOOR_DEFAULT")
        return CalibrationPresets.OUTDOOR_DEFAULT
    }

    // 🚨 NEW HELPER FUNCTION: Get or create KF
    private fun getOrCreateKalmanFilter(objectId: Int, initialDistance: Float): SimpleKalmanFilter {
        return kalmanFilters.getOrPut(objectId) {
            // Tuning parameters for a stable estimate

            // >>>>>>>>> CHANGE R AND Q IF NEEDED <<<<<<<<<<<<<<< //
            val initialP = 5.0f      // High initial uncertainty
            val R =  0.5f           // Measurement Noise (R) -> MiDaS sensor noise
            val Q =  0.1f           // Process Noise (Q) -> Object movement noise

            SimpleKalmanFilter(
                estimatedDistance = initialDistance,
                estimatedError = initialP,
                measurementNoise = R,
                processNoise = Q
            )
        }
    }

    // --- UPDATED: ASSIGN DEPTH WITH DYNAMIC A/B and KALMAN FILTERING ---
    private fun assignDepthToObjects(
        outputs: List<ModelOutput>,
        depthMap: Array<Array<Array<ByteArray>>>, // [1][256][256][1]
        constants: CalibrationConstants
    ) {
        val midasFrameSize = 256
        val regionSize = 5
        val half = regionSize / 2

        // Use the dynamically selected constants
        val A = constants.A
        val B = constants.B

        for ((index, output) in outputs.withIndex()) { // Use 'index' as a temporary ID
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

            // STEP 1: FIXED DE-QUANTIZATION
            val relativeDepthValue = OUTPUT_SCALE * (medianQuantized - ZERO_POINT)

            // STEP 2: DYNAMIC METRIC CONVERSION (Calculates the raw, noisy distance)
            val inverseDistance = (A * relativeDepthValue) + B
            val rawZMeters: Float

            if (inverseDistance > 0.001f) {
                rawZMeters = 1.0f / inverseDistance
            } else {
                rawZMeters = 10.0f
            }

            // --- 🚨 STEP 3: KALMAN FILTERING ---

            // 1. Get/Create the filter for this object (using index as a temporary ID)
            val filter = getOrCreateKalmanFilter(index, rawZMeters)

            // 2. Predict the next state
            filter.predict()

            // 3. Update the filter with the new noisy measurement (rawZMeters)
            val smoothedDistance = filter.update(rawZMeters)

            // Apply the final smoothed distance to the output
            output.distance.value = smoothedDistance

            Log.d("METRIC_DISTANCE", "[KF] Object: ${output.name} | Raw: ${"%.2f".format(rawZMeters)}m | Smoothed: ${"%.2f".format(smoothedDistance)}m")
        }
    }
}