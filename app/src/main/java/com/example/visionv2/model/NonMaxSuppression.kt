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

    val box1Left = box1.centerX - box1.width / 2
    val box1Top = box1.centerY - box1.height / 2
    val box1Right = box1.centerX + box1.width / 2
    val box1Bottom = box1.centerY + box1.height / 2

    val box2Left = box2.centerX - box2.width / 2
    val box2Top = box2.centerY - box2.height / 2
    val box2Right = box2.centerX + box2.width / 2
    val box2Bottom = box2.centerY + box2.height / 2

    val x1 = max(box1Left, box2Left)
    val y1 = max(box1Top, box2Top)
    val x2 = min(box1Right, box2Right)
    val y2 = min(box1Bottom, box2Bottom)

    val intersectionWidth = max(0f, x2 - x1)
    val intersectionHeight = max(0f, y2 - y1)
    val intersectionArea = intersectionWidth * intersectionHeight

    val box1Area = (box1Right - box1Left) * (box1Bottom - box1Top)
    val box2Area = (box2Right - box2Left) * (box2Bottom - box2Top)

    val unionArea = box1Area + box2Area - intersectionArea

    return if (unionArea > 0) intersectionArea / unionArea else 0f
}