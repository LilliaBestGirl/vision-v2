package com.example.visionv2.presentation.camera

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors

@Suppress("DEPRECATION")
class CameraController(
    private val context: Context,
    private val analyzer: ImageAnalysis.Analyzer,
    screenWidth: Float,
    screenHeight: Float,
) {

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private val imageAnalysis = ImageAnalysis.Builder()
        .setTargetResolution(Size(screenWidth.toInt(), screenHeight.toInt()))
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .also {
            it.setAnalyzer(cameraExecutor, analyzer)
        }

    private val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    fun startCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
        }, ContextCompat.getMainExecutor(context))
    }

    fun stopCamera() {
        cameraProviderFuture.get().unbindAll()
        cameraExecutor.shutdown()
    }
}