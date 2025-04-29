package com.example.visionv2.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Paint
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.visionv2.data.ModelOutput
import com.example.visionv2.domain.ARRenderer
import com.example.visionv2.presentation.camera.BackgroundRenderer
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableException

class MainActivity : ComponentActivity() {

    var installRequested = false
    private lateinit var session: Session
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: ARRenderer
    private lateinit var backgroundRenderer: BackgroundRenderer

    override fun onResume() {
        super.onResume()

        if (!hasCameraPermission()) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), 0
            )
            return
        }

        try {
            when (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    installRequested = true
                    return
                }

                ArCoreApk.InstallStatus.INSTALLED -> {
                    installRequested = false

                    if (!::session.isInitialized) {
                        session = Session(this)
                        Log.d("ARCore", "Session initialized")
                    }

                    val config = Config(session)
                    config.depthMode = Config.DepthMode.AUTOMATIC
                    session.configure(config)

                    if (!::backgroundRenderer.isInitialized) {
                        backgroundRenderer = BackgroundRenderer()

                        Log.d("bgRenderer", "Background Renderer created")
                    }

                    renderer = ARRenderer(session, backgroundRenderer)

                    glSurfaceView.setRenderer(renderer)
                    glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

                    session.resume()
                    glSurfaceView.onResume()

                    Log.d("ARCore", "Session resumed with renderer")
                }
            }
        } catch (e: UnavailableException) {
            Log.e("ARCore", "ARCore not available: ${e.message}")
            Toast.makeText(this, "ARCore is required", Toast.LENGTH_LONG).show()
            finish()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        glSurfaceView = GLSurfaceView(this)
        glSurfaceView.setEGLContextClientVersion(3)

        setContentView(glSurfaceView)

//        setContent {
//            val configuration = LocalConfiguration.current
//            val density = LocalDensity.current
//
//            val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
//            val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
//
//            var detections = remember {
//                mutableStateListOf<ModelOutput>()
//            }
//
//            val analyzer = remember {
//                FrameAnalyzer(
//                    context = applicationContext,
//                    detector = ObjectDetectorModel(
//                        context = applicationContext
//                    ),
//                    screenWidth = screenWidth,
//                    screenHeight = screenHeight,
//                    onResults = {
//                        detections.clear()
//                        detections.addAll(it)
//                    }
//                )
//            }
//
//            val controller = CameraController(applicationContext, analyzer, screenWidth, screenHeight)
//
//            VISIONV2Theme {
//                Box(Modifier.fillMaxSize()) {
//                    GLView(
//                        onSurfaceReady = { surface -> glSurfaceView = surface },
//                        renderer = renderer
//                    )
//
//                    CameraPreview(controller, modifier = Modifier.fillMaxSize())
//
//                    BoundingBoxCanvas(detections)
//                }
//            }
//        }
    }

    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun BoundingBoxCanvas(detections: List<ModelOutput>) {
    Canvas(modifier = Modifier.fillMaxSize()) {

        detections.forEach { detection ->
            val left = detection.centerX - detection.width / 2
            val top = detection.centerY - detection.height / 2
            val right = detection.centerX + detection.width / 2
            val bottom = detection.centerY + detection.height / 2

            val score = String.format("%.2f", detection.score)

            Log.d(
                "BoundingBoxCanvas",
                "Drawing bounding box: Left: $left, Top: $top, Right: $right, Bottom: $bottom"
            )

            drawRect(
                color = Color.Red,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(5f)
            )

            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    "${detection.name} - $score",
                    left,
                    top - 10f,
                    Paint().apply {
                        color = android.graphics.Color.RED
                        textSize = 40f
                    }
                )
            }
        }
    }
}