package com.example.visionv2.data

import java.nio.ByteBuffer

data class PreprocessResult(
    val inputBuffer: ByteBuffer,
    val xOffset: Int,
    val yOffset: Int,
    val scale: Float
)
