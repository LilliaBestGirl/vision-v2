package com.example.visionv2.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.speech.tts.TextToSpeech.OnInitListener
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.visionv2.data.ModelOutput
import com.example.visionv2.domain.DepthEstimation
import com.example.visionv2.domain.FrameAnalyzer
import com.example.visionv2.tts.TTSHelper
import com.example.visionv2.model.ObjectDetectorModel
import com.example.visionv2.presentation.camera.CameraController
import com.example.visionv2.presentation.camera.CameraPreview
import com.example.visionv2.ui.theme.VISIONV2Theme

class MainActivity : ComponentActivity() {

    private lateinit var tts: TTSHelper

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

        tts = TTSHelper(this)
        Toast.makeText(this, "TTS Initialized", Toast.LENGTH_SHORT).show()

        setContent {
            val configuration = LocalConfiguration.current
            val density = LocalDensity.current

            val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
            val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

            val detections = remember { mutableStateListOf<ModelOutput>() }

            val detector = ObjectDetectorModel(applicationContext)

            val analyzer =
                FrameAnalyzer(
                    context = applicationContext,
                    detector = detector,
                    depth = DepthEstimation(context = applicationContext),
                    tts = tts,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    onResults = {
                        detections.clear()
                        detections.addAll(it)
                    }
                )

            val controller = CameraController(applicationContext, analyzer, screenWidth, screenHeight)

            VISIONV2Theme {
                Box(Modifier.fillMaxSize()) {
                    CameraPreview(controller, modifier = Modifier.fillMaxSize())
                    
                    BoundingBoxCanvas(detections)

                    LanguageButton(tts, applicationContext)
                }
            }
        }
    }

    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun LanguageButton(tts: TTSHelper, context: Context) {
    val expanded = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        contentAlignment = Alignment.TopEnd
    ) {
        IconButton(onClick = { expanded.value = !expanded.value }) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Language Options",
                modifier = Modifier.size(32.dp)
            )
        }
        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false },
            offset = DpOffset(x = (210).dp, y = 0.dp)
        ) {
            DropdownMenuItem(
                text = { Text("English") },
                onClick = {
                    tts.changeLanguage("en", "US")
                    Toast.makeText(context, "Changed to EN", Toast.LENGTH_SHORT).show()
                    expanded.value = !expanded.value
                }
            )
            DropdownMenuItem(
                text = { Text("Filipino") },
                onClick = {
                    tts.changeLanguage("fil", "PH")
                    Toast.makeText(context, "Changed to PH", Toast.LENGTH_SHORT).show()
                    expanded.value = !expanded.value
                }
            )
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun BoundingBoxCanvas(detections: List<ModelOutput>) {
    Canvas(modifier = Modifier.fillMaxSize()) {

        detections.forEach { detection ->
            val left = detection.centerX - detection.width / 2
            val top = detection.centerY - detection.height / 2
            val right = detection.centerX + detection.width / 2
            val bottom = detection.centerY + detection.height / 2

            val score = String.format("%.2f", detection.score)
            val depthValue = detection.distance.value

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
                    "${detection.name} - $score - $depthValue",
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