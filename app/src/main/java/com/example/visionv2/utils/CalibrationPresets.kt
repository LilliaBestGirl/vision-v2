package com.example.visionv2.utils

object CalibrationPresets {

    // --- 1. OUTDOOR Calibration (Inverse Depth: D = 1 / (A*M + B)) ---
    // Uses your existing constants which you verified work well for outdoor scenes.
    val OUTDOOR_DEFAULT = CalibrationConstants(
        A = 0.001505f,
        B = -0.1669f
    )

    // --- 2. INDOOR Calibration (Inverse Depth: D = 1 / (A*M + B)) ---
    // !!! THESE ARE PLACEHOLDER VALUES - YOU MUST REPLACE THEM !!!
    // Use the values derived from your indoor object measurements (linear regression).
    val INDOOR_DEFAULT = CalibrationConstants(
        A = 0.002222f, // Placeholder for your new Indoor A
        B = 0.0001f  // Placeholder for your new Indoor B
    )

    // ----------------------------------------------------------------------
    // 3. SEMANTIC CALIBRATION CLASSES (Used to decide which A/B set to use)
    // ----------------------------------------------------------------------

    // Classes that strongly suggest an open, wide environment.
    val SEMANTIC_OUTDOOR_CLASSES = setOf(
        "traffic light", "bicycle", "bus", "street light",
        "motorcycle", "truck", "car"
    )

    // Classes that strongly suggest a bounded, close environment.
    val SEMANTIC_INDOOR_CLASSES = setOf(
        "sink", "chair", "couch", "door", "bed", "refrigerator",
        "table", "television", "toilet"
    )

    // You can add heuristic constants here if you want to use the depth-range check later.
}