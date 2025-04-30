package com.example.visionv2.domain

import android.content.Context
import android.graphics.Bitmap
import com.example.visionv2.data.ModelOutput

interface Depth {
    fun depth(bitmap: Bitmap, modelOutput: List<ModelOutput>)
}