package com.example.visionv2.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasCameraPermission()) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), 0
            )
        }

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
                    detector = ObjectDetectorModel(
                        context = applicationContext
                    ),
                    onResults = { detections = it }
                )
            }

            val controller = CameraController(applicationContext, analyzer, screenWidth, screenHeight)

            VISIONV2Theme {
                CameraPreview(controller, modifier = Modifier.fillMaxSize())
                BoundingBoxCanvas(detections)
            }
        }
    }
    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun BoundingBoxCanvas(detections: List<ModelOutput>,) {
    Canvas(modifier = Modifier.fillMaxSize()) {

        detections.forEach { detection ->
            val left = detection.left
            val top = detection.top
            val right = detection.right
            val bottom = detection.bottom

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

@Preview
@Composable
fun BoundingBoxPreview() {
    val mockDetections = listOf(
        ModelOutput(
            left = 0.1f, top = 0.1f, right = 0.4f, bottom = 0.4f,
            score = 0.85f, classId = 1, name = "Person"
        ),
        ModelOutput(
            left = 0.5f, top = 0.2f, right = 0.8f, bottom = 0.5f,
            score = 0.92f, classId = 2, name = "Dog"
        )
    )

    BoundingBoxCanvas(mockDetections)
}