package com.example.visionv2.domain

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import com.example.visionv2.presentation.camera.BackgroundRenderer
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.NotYetAvailableException
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ARRenderer(
    private val session: Session
) : GLSurfaceView.Renderer {
    private val backgroundRenderer = BackgroundRenderer()

    fun getTextureId(): Int = backgroundRenderer.textureId

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        backgroundRenderer.createOnGlThread()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // Retrieve the AR frame
        val frame = session.update()
        val camera = frame.camera

        // Make sure that we have a valid camera texture
        if (camera.trackingState == TrackingState.TRACKING) {
            // Here we are binding the texture and passing it to ARCore's frame.
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, backgroundRenderer.textureId)

            // Draw the background texture (your camera feed)
            backgroundRenderer.draw(frame) // Use this method to actually render the camera frame to the texture
        } else {
            // Optionally log or handle cases where ARCore isn't tracking
            Log.d("ARRenderer", "Camera is not tracking!")
        }

//        val scaleX = depthWidth.toFloat() / screenWidth
//        val scaleY = depthHeight.toFloat() / screenHeight
//
//        // Update distances on model results
//        synchronized(modelResults) {
//            modelResults.forEach { result ->
//                val depthX = (result.centerX * scaleX).toInt()
//                val depthY = (result.centerY * scaleY).toInt()
//
//                val plane = depthImage.planes[0]
//                val buffer = plane.buffer
//                val rowStride = plane.rowStride
//                val pixelStride = plane.pixelStride
//
//                val offset = depthY * rowStride + depthX * pixelStride
//
//                try {
//                    val depthMillimeters = buffer.getShort(offset).toInt() and 0xFFFF
//                    result.distance.value = depthMillimeters / 1000f
//                } catch (e: Exception) {
//                    result.distance.value = null
//                }
//            }
//        }
    }
}