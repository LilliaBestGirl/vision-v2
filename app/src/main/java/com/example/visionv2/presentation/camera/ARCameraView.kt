package com.example.visionv2.presentation.camera

import android.view.SurfaceView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun ARCameraView(surfaceView: SurfaceView) {
    AndroidView(
        factory = { surfaceView },
        modifier = Modifier.fillMaxSize()
    )
}