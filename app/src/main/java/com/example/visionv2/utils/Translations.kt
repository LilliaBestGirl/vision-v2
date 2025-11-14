package com.example.visionv2.utils

object Translations {
    enum class Language(val code: String) {
        ENGLISH("en-US"),
        FILIPINO("fil-PH");

        companion object {
            fun fromCode(code: String): Language {
                return Language.entries.find { it.code == code } ?: ENGLISH
            }
        }
    }

    private const val FAR_THRESHOLD_M = 4.0f        // > 4 meters range
    private const val MEDIUM_FAR_THRESHOLD_M = 3.0f // 3 - 4 meters range
    private const val MEDIUM_THRESHOLD_4 = 2.6f     // 2.6 - 3 meters range
    private const val MEDIUM_THRESHOLD_3 = 2.1f     // 2.1 - 2.5 meters range
    private const val MEDIUM_THRESHOLD_2 = 1.6f     // 1.6 - 2 meters range
    private const val CLOSE_THRESHOLD_M = 1.0f      // 1 - 1.5 meters range

    // Label Maps (unchanged)
    private val englishLabelMap = listOf(
        "Sink", "Traffic light", "Bicycle", "Bus", "Person", "Chair",
        "Couch", "Door", "Street light", "Bed", "Refrigerator",
        "Motorcycle", "Table", "Television", "Truck", "Toilet",
        "Bench", "Car", "Stairs"
    )

    private val filipinoLabelMap = listOf(
        "Lababo", "Ilaw trapiko", "Bisikleta", "Bus", "Tao", "Silya",
        "Sala", "Pinto", "Ilaw sa kalye", "Kama", "Refrigerator",
        "Motorsiklo", "Mesa", "Telebisyon", "Trak", "Kubeta",
        "Bangko", "Kotse", "Hagdan"
    )

    private val englishDistanceMap = mapOf(
        "very_close" to "less than 1 meter away",
        "close_1" to "1 to 1.5 meters away",
        "close_2" to "1.6 to 2 meters away",
        "medium_3" to "2.1 to 2.5 meters away",
        "medium_4" to "2.6 to 3 meters away",
        "medium_far" to "3 to 4 meters away",
        "far" to "more than 4 meters away"
    )

    private val filipinoDistanceMap = mapOf(
        "very_close" to "mas mababa sa isang metro ang layo",
        "close_1" to "isa hanggang isa't kalahating metro ang layo",
        "close_2" to "isa't anim hanggang dalawang metro ang layo",
        "medium_3" to "dalawa at isa hanggang dalawa't kalahating metro ang layo",
        "medium_4" to "dalawa't anim hanggang tatlong metro ang layo",
        "medium_far" to "tatlo hanggang apat na metro ang layo",
        "far" to "higit pa sa apat na metro ang layo"
    )

    private val templates = mapOf(
        Language.ENGLISH to "%s detected, %s",
        Language.FILIPINO to "Mayroong %s, %s"
    )

    fun getLabel(index: Int, language: Language): String {
        return when (language) {
            Language.ENGLISH -> englishLabelMap.getOrNull(index) ?: "Unknown"
            Language.FILIPINO -> filipinoLabelMap.getOrNull(index) ?: "Hindi alam"
        }
    }

    fun getDistanceLabel(depthValue: Float, language: Language): String {
        val key = when {
            depthValue >= FAR_THRESHOLD_M -> "far"
            // 3.0f to 3.99f
            depthValue >= MEDIUM_FAR_THRESHOLD_M -> "medium_far"
            // 2.6f to 2.99f
            depthValue >= MEDIUM_THRESHOLD_4 -> "medium_4"
            // 2.1f to 2.59f
            depthValue >= MEDIUM_THRESHOLD_3 -> "medium_3"
            // 1.6f to 2.09f
            depthValue >= MEDIUM_THRESHOLD_2 -> "close_2"
            // 1.0f to 1.59f
            depthValue >= CLOSE_THRESHOLD_M -> "close_1"
            else -> "very_close"
        }

        return when (language) {
            Language.ENGLISH -> englishDistanceMap[key] ?: "unknown distance"
            Language.FILIPINO -> filipinoDistanceMap[key] ?: "hindi alam ang distansya"
        }
    }

    fun getSentence(label: String, distance: String, language: Language): String {
        return String.format(templates[language] ?: "%s detected, %s", label, distance)
    }
}