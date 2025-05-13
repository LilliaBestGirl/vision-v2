package com.example.visionv2.domain

import android.graphics.Bitmap
import com.example.visionv2.data.ModelOutput

interface Detector {
    fun detect(bitmap: Bitmap): List<ModelOutput>
    fun close()
}