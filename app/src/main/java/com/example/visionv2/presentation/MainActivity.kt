package com.example.visionv2.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.visionv2.data.ModelOutput
import com.example.visionv2.domain.DepthEstimation
import com.example.visionv2.domain.FrameAnalyzer
import com.example.visionv2.model.ObjectDetectorModel
import com.example.visionv2.presentation.camera.CameraController
import com.example.visionv2.presentation.camera.CameraPreview
import com.example.visionv2.ui.theme.VISIONV2Theme

class MainActivity : ComponentActivity() {

    override fun onResume() {
        super.onResume()

        if (!hasCameraPermission()) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), 0
            )
            return
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val configuration = LocalConfiguration.current
            val density = LocalDensity.current

            val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
            val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

            var isIndoorMode by remember { mutableStateOf(false) }
            var detections = remember { mutableStateListOf<ModelOutput>() }

            val detectorState = remember(isIndoorMode) {
                Log.d("detectorState", "Indoor Mode: $isIndoorMode")
                ObjectDetectorModel(applicationContext, isIndoorMode)
            }

            val analyzer = remember(detectorState) {
                Log.d("analyzer", "${detectorState.isIndoorMode}")
                FrameAnalyzer(
                    context = applicationContext,
                    detector = detectorState,
                    depth = DepthEstimation(context = applicationContext),
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    onResults = {
                        detections.clear()
                        detections.addAll(it)
                    }
                )
            }

            val controller = CameraController(applicationContext, analyzer, screenWidth, screenHeight)

            LaunchedEffect(isIndoorMode) {
                val newDetector = ObjectDetectorModel(applicationContext, isIndoorMode)
                Log.d("DetectorInit", "Created new detector: ${System.identityHashCode(newDetector)}, isIndoor: ${newDetector.isIndoorMode}")

                analyzer.updateDetector(newDetector)

                // Required to ensure CameraX continues to use updated analyzer instance
                controller.updateAnalyzer(analyzer)
            }

            VISIONV2Theme {
                Box(Modifier.fillMaxSize()) {
                    CameraPreview(controller, modifier = Modifier.fillMaxSize())

                    BoundingBoxCanvas(detections)

                    Switch(
                        checked = isIndoorMode,
                        onCheckedChange = {
                            isIndoorMode = it
                            Log.d("Switch", "Switch clicked, indoor mode is now: $isIndoorMode")


                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
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