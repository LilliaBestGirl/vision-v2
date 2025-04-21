package com.example.visionv2.data

data class ModelOutput(
    val centerX: Float,
    val centerY: Float,
    val width: Float,
    val height: Float,
    val score: Float,
    val classId: Int,
    val name: String,
    var distance: Float? = null,
)
