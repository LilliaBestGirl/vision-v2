package com.example.visionv2.domain

import android.util.Log
import com.example.visionv2.data.ModelOutput
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.NotYetAvailableException

fun DepthAPI(
    results: List<ModelOutput>,
    session: Session?,
    screenWidth: Float,
    screenHeight: Float
) {
    results.forEach { result ->
        if (session == null) {
            Log.d("Depth", "Session is null")
        }

        val currentSession = session ?: return

        val frame = currentSession.update()
        if (frame.camera.trackingState != TrackingState.TRACKING) {
            Log.d("Depth", "Camera not tracking")
            return
        }

        val depthImage = try {
            frame.acquireDepthImage16Bits()
        } catch (e: NotYetAvailableException) {
            Log.d("Depth", "Depth image not available yet")
            return
        }

        val depthWidth = depthImage.width
        val depthHeight = depthImage.height

        Log.d("Depth", "Depth Image Size: $depthWidth x $depthHeight")

        val scaleX = depthWidth.toFloat() / screenWidth
        val scaleY = depthHeight.toFloat() / screenHeight

        val depthX = (result.centerX * scaleX).toInt()
        val depthY = (result.centerY * scaleY).toInt()

        val buffer = depthImage.planes[0].buffer
        val index = depthY * depthWidth + depthX
        val depthMillimeters = buffer.getShort(index * 2).toInt() and 0xFFFF

        result.distance = depthMillimeters / 1000f
    }
}