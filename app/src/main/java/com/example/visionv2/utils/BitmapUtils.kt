package com.example.visionv2.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.example.visionv2.data.PreprocessResult
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

private var xOffset = 0
private var yOffset = 0
private var scale = 0f

fun preprocessBitmap(
    bitmap: Bitmap,
    targetSize: Int
    ): PreprocessResult {
    val originalWidth = bitmap.width
    val originalHeight = bitmap.height

    scale = min(targetSize.toFloat() / originalWidth, targetSize.toFloat() / originalHeight)
    val newWidth = (originalWidth * scale).toInt()
    val newHeight = (originalHeight * scale).toInt()

    val resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

    xOffset = (targetSize - newWidth) / 2
    yOffset = (targetSize - newHeight) / 2

    val paddedBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(paddedBitmap)
    val paint = Paint(Paint.FILTER_BITMAP_FLAG)

    canvas.drawColor(Color.BLACK)
    canvas.drawBitmap(resizedBitmap, xOffset.toFloat(), yOffset.toFloat(), paint)

    val inputBuffer = ByteBuffer.allocateDirect(targetSize * targetSize * 3 * 4)
    inputBuffer.order(ByteOrder.nativeOrder())

    val intValues = IntArray(targetSize * targetSize)
    paddedBitmap.getPixels(intValues, 0, targetSize, 0, 0, targetSize, targetSize)

    Log.d("Padded Bitmap", "${paddedBitmap.width} x ${paddedBitmap.height}")

    for (pixel in intValues) {
        val r = (pixel shr 16 and 0xFF) / 255.0f
        val g = (pixel shr 8 and 0xFF) / 255.0f
        val b = (pixel and 0xFF) / 255.0f

        inputBuffer.putFloat(r)
        inputBuffer.putFloat(g)
        inputBuffer.putFloat(b)
    }

    return PreprocessResult(inputBuffer, xOffset, yOffset, scale)
}

fun preprocessBitmapMidas(
    bitmap: Bitmap
): TensorImage {
    // Create TensorImage from bitmap
    var tensorImage = TensorImage.fromBitmap(bitmap)

    // Build preprocessing pipeline
    val imageProcessor = ImageProcessor.Builder()
        // Pad to square to avoid aspect ratio distortion
        .add(
            ResizeWithCropOrPadOp(
                maxOf(bitmap.width, bitmap.height),
                maxOf(bitmap.width, bitmap.height)
            )
        )
        // Resize to model input
        .add(ResizeOp(256, 256, ResizeOp.ResizeMethod.BILINEAR))
        // Normalize to MiDaS expected mean/std
        .add(
            NormalizeOp(
                floatArrayOf(0.485f, 0.456f, 0.406f),
                floatArrayOf(0.229f, 0.224f, 0.225f)
            )
        )
        .build()

    // Apply preprocessing pipeline
    tensorImage = imageProcessor.process(tensorImage)

    return tensorImage
}

fun preprocessBitmapYolo(
    bitmap: Bitmap
): TensorImage {
    var tensorImage = TensorImage.fromBitmap(bitmap)

    // YOLOv5 expects square input (usually 640x640)
    val imageProcessor = ImageProcessor.Builder()
        .add(ResizeWithCropOrPadOp(
            maxOf(bitmap.width, bitmap.height),
            maxOf(bitmap.width, bitmap.height)
        ))
        .add(ResizeOp(640, 640, ResizeOp.ResizeMethod.BILINEAR))
        // Normalize pixels to [0,1]
        .add(NormalizeOp(0f, 255f))
        .build()

    tensorImage = imageProcessor.process(tensorImage)

    return tensorImage
}