package com.example.visionv2.domain

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import com.example.visionv2.presentation.camera.BackgroundRenderer
import com.google.ar.core.Session
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ARRenderer(
    private val session: Session,
    private val backgroundRenderer: BackgroundRenderer
) : GLSurfaceView.Renderer {

    fun getTextureId(): Int = backgroundRenderer.textureId

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)

        val extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS)
        Log.d("GLCheck", "Supported OpenGL Extensions: $extensions")

        if (extensions?.contains("GL_OES_EGL_image_external") == true) {
            Log.d("GLCheck", "GL_OES_EGL_image_external is supported!")
        } else {
            Log.e("GLCheck", "GL_OES_EGL_image_external is NOT supported! Camera texture won't render.")
        }

        backgroundRenderer.createOnGlThread()
        session.setCameraTextureName(getTextureId())
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val frame = session.update()

        backgroundRenderer.draw(frame)

//        Below is the code block for depth estimation

//        val camera = frame.camera
//
//        if (camera.trackingState == TrackingState.TRACKING) {
//            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, backgroundRenderer.textureId)
//
//            backgroundRenderer.draw(frame)
//        } else {
//            Log.d("ARRenderer", "Camera is not tracking!")
//        }

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