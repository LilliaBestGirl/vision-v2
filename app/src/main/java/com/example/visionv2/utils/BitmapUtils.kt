package com.example.visionv2.utils

import android.graphics.Bitmap
import com.example.visionv2.data.PreprocessResult
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import kotlin.math.max
import kotlin.math.min

fun preprocessBitmapYolo(
    bitmap: Bitmap,
    targetSize: Int
): PreprocessResult {
    val originalWidth = bitmap.width
    val originalHeight = bitmap.height

    val scale = min(
        targetSize.toFloat() / originalWidth,
        targetSize.toFloat() / originalHeight
    )
    val newWidth = (originalWidth * scale).toInt()
    val newHeight = (originalHeight * scale).toInt()

    val xOffset = (targetSize - newWidth) / 2
    val yOffset = (targetSize - newHeight) / 2

    var tensorImage = TensorImage(DataType.FLOAT32)
    tensorImage.load(bitmap)

    val imageProcessor = ImageProcessor.Builder()
        .add(ResizeWithCropOrPadOp(
            maxOf(bitmap.width, bitmap.height),
            maxOf(bitmap.width, bitmap.height)
        ))
        .add(ResizeOp(targetSize, targetSize, ResizeOp.ResizeMethod.BILINEAR))
        .add(NormalizeOp(0f, 255f))
        .build()

    tensorImage = imageProcessor.process(tensorImage)

    val inputBuffer = tensorImage.buffer

    return PreprocessResult(inputBuffer, xOffset, yOffset, scale)
}

fun preprocessBitmapMidas(
    bitmap: Bitmap,
    targetSize: Int
): PreprocessResult {
    val originalWidth = bitmap.width
    val originalHeight = bitmap.height

    val scale = targetSize.toFloat() / max(originalWidth, originalHeight)
    val newWidth = originalWidth * scale
    val newHeight = originalHeight * scale
    val xOffset = (targetSize - newWidth) / 2f
    val yOffset = (targetSize - newHeight) / 2f

    var tensorImage = TensorImage(DataType.FLOAT32)
    tensorImage.load(bitmap)

    val imageProcessor = ImageProcessor.Builder()
        .add(ResizeWithCropOrPadOp(
            maxOf(bitmap.width, bitmap.height),
            maxOf(bitmap.width, bitmap.height)
        ))
        .add(ResizeOp(targetSize, targetSize, ResizeOp.ResizeMethod.BILINEAR))
        .add(NormalizeOp(
            floatArrayOf(0.485f, 0.456f, 0.406f),
            floatArrayOf(0.229f, 0.224f, 0.225f)
        ))
        .build()

    tensorImage = imageProcessor.process(tensorImage)

    val inputBuffer = tensorImage.buffer

    return PreprocessResult(inputBuffer, xOffset.toInt(), yOffset.toInt(), scale)
}