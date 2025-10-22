package com.example.visionv2.data

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

data class ModelOutput(
    val centerX: Float,
    val centerY: Float,
    val width: Float,
    val height: Float,
    val score: Float,
    val classId: Int,
    val name: String,
    var distance: MutableState<Float?> = mutableStateOf(null)
)
