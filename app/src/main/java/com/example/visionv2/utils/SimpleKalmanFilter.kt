package com.example.visionv2.utils

class SimpleKalmanFilter(
    private var estimatedDistance: Float, // The current best estimate (x)
    private var estimatedError: Float,    // Current uncertainty (P)
    private val measurementNoise: Float,  // Sensor noise (R): How noisy the MiDaS measurement is
    private val processNoise: Float       // Process noise (Q): How much the object is allowed to move
) {
    // Prediction Step
    fun predict(): Float {
        // P = P + Q (Uncertainty grows as time passes)
        estimatedError += processNoise
        // x' = x (Predicted distance is the last estimated distance, assuming constant position)
        return estimatedDistance
    }

    // Update Step
    fun update(measuredDistance: Float): Float {
        // 1. Kalman Gain (K): K = P / (P + R) -> How much to trust the measurement (R) vs. the prediction (P)
        val kalmanGain = estimatedError / (estimatedError + measurementNoise)

        // 2. State Update (x): x = x + K * (measurement - x)
        estimatedDistance += kalmanGain * (measuredDistance - estimatedDistance)

        // 3. Error Update (P): P = (1 - K) * P (Uncertainty is reduced after a measurement)
        estimatedError *= (1 - kalmanGain)

        return estimatedDistance
    }
}