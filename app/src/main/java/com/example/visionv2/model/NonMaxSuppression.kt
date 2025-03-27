package com.example.visionv2.model

import android.util.Log
import com.example.visionv2.data.ModelOutput
import kotlin.math.max
import kotlin.math.min

fun nonMaxSuppression(
    boxesList: List<ModelOutput>,
    iouThreshold: Float = 0.5f
): List<ModelOutput> {
    val sortedBoxes = boxesList.sortedByDescending { it.score }.toMutableList()
    val finalBoxes = mutableListOf<ModelOutput>()

    while (sortedBoxes.isNotEmpty()) {
        val bestBox = sortedBoxes.removeAt(0)
        Log.d("NMS", "Best Box: ${bestBox.name} - ${bestBox.score}")
        finalBoxes.add(bestBox)

        sortedBoxes.removeAll { otherBox ->
            iou(bestBox, otherBox) > iouThreshold
        }
    }

    return finalBoxes
}

private fun iou(
    box1: ModelOutput,
    box2: ModelOutput,
): Float {
    val x1 = max(box1.left, box2.left)
    val y1 = max(box1.top, box2.top)
    val x2 = min(box1.right, box2.right)
    val y2 = min(box1.bottom, box2.bottom)

    val intersectionWidth = max(0f, x2 - x1)
    val intersectionHeight = max(0f, y2 - y1)
    val intersectionArea = intersectionWidth * intersectionHeight

    val box1Area = (box1.right - box1.left) * (box1.bottom - box1.top)
    val box2Area = (box2.right - box2.left) * (box2.bottom - box2.top)

    val unionArea = box1Area + box2Area - intersectionArea

    return if (unionArea > 0) intersectionArea / unionArea else 0f
}