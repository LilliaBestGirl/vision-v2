package com.example.visionv2.data

data class ModelOutput(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val score: Float,
    val classId: Int,
    val name: String
)
