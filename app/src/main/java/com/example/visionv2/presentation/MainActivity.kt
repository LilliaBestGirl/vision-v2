package com.example.visionv2.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.visionv2.data.ModelOutput
import com.example.visionv2.domain.FrameAnalyzer
import com.example.visionv2.model.ObjectDetectorModel
import com.example.visionv2.presentation.camera.CameraController
import com.example.visionv2.presentation.camera.CameraPreview
import com.example.visionv2.ui.theme.VISIONV2Theme
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableException

class MainActivity : ComponentActivity() {

    private var installRequested = false
    private var session: Session? = null

    override fun onResume() {
        super.onResume()

        Log.d("Lifecycle", "onResume called")

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

                    if (session == null) {
                        session = Session(this)
                        Log.d("ARCoreInstall", "ARCore session installed successfully")
                    }

                    session?.resume()
                    Log.d("ARCore", "Session resumed")
                }
            }
        } catch (e: UnavailableException) {
            Log.e("ARCore", "ARCore not available: ${e.message}")
            Toast.makeText(this, "ARCore is required for this app to run", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val configuration = LocalConfiguration.current
            val density = LocalDensity.current

            val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
            val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

            var detections by remember {
                mutableStateOf(emptyList<ModelOutput>())
            }

            val analyzer = remember {
                FrameAnalyzer(
                    context = applicationContext,
                    detector = ObjectDetectorModel(
                        context = applicationContext
                    ),
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    arSession = session,
                    onResults = { detections = it }
                )
            }

            val controller = CameraController(applicationContext, analyzer, screenWidth, screenHeight)

            VISIONV2Theme {
                Box(Modifier.fillMaxSize()) {
                    CameraPreview(controller, modifier = Modifier.fillMaxSize())

                    BoundingBoxCanvas(detections)
                }
            }
        }
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