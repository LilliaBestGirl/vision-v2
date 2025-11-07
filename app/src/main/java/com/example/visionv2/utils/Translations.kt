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

    private const val VERY_CLOSE_THRESHOLD = 800f
    private const val CLOSE_THRESHOLD = 500f
    private const val MEDIUM_THRESHOLD = 300f

    private const val THRESHOLD_1M = 0.78f
    private const val THRESHOLD_1_5M = 0.53f
    private const val THRESHOLD_2M = 0.32f
    private const val THRESHOLD_2_5M = 0.18f
    private const val THRESHOLD_3M = 0.14f

    // Label Maps
    private val englishLabelMap = listOf(
        "Sink",
        "Traffic light",
        "Bicycle",
        "Bus",
        "Person",
        "Chair",
        "Couch",
        "Door",
        "Street light",
        "Bed",
        "Refrigerator",
        "Motorcycle",
        "Table",
        "Television",
        "Truck",
        "Toilet",
        "Bench",
        "Car",
        "Stairs"
    )

    private val filipinoLabelMap = listOf(
        "Lababo",
        "Ilaw trapiko",
        "Bisikleta",
        "Bus",
        "Tao",
        "Silya",
        "Sala",
        "Pinto",
        "Ilaw sa kalye",
        "Kama",
        "Refrigerator",
        "Motorsiklo",
        "Mesa",
        "Telebisyon",
        "Trak",
        "Kubeta",
        "Bangko",
        "Kotse",
        "Hagdan"
    )

    // Distance descriptions
    private val englishDistanceMap = mapOf(
        "very_close" to "less than 1 meter away",
        "close" to "1.5 to 3 meters away",
        "medium" to "3.5 to 4 meters away",
        "far" to "5 meters away"
    )

    // Just in case, will change depending on the results of the test
    private val distanceMap = listOf(
        "very_close" to "1m away",
        "close" to "1.5m away",
        "moderate" to "2m away",
        "medium" to "2.5m away",
        "far" to "3m away",
    )

    private val filipinoDistanceMap = mapOf(
        "very_close" to "mas mababa sa isang metro ang layo",
        "close" to "isa't kalahati hanggang tatlong metro ang layo",
        "medium" to "tatlo't kalahati hanggang apat na metro ang layo",
        "far" to "lima o mahigit pang metro ang layo"
    )

    // Sentence templates
    private val templates = mapOf(
        Language.ENGLISH to "%s detected, %s",
        Language.FILIPINO to "Mayroong %s, %s"
    )

    // Common phrases
    private val startupMessages = mapOf(
        Language.ENGLISH to "Welcome to VISION, detecting objects now",
        Language.FILIPINO to "Maligayang pagdating sa VISION, nagsisimula ang pagtukoy ng mga bagay"
    )

    private val noDetectionMessages = mapOf(
        Language.ENGLISH to "No objects detected",
        Language.FILIPINO to "Walang nakitang bagay"
    )

    // ---- Retrieval Functions ----

    fun getLabel(index: Int, language: Language): String {
        return when (language) {
            Language.ENGLISH -> englishLabelMap.getOrNull(index) ?: "Unknown"
            Language.FILIPINO -> filipinoLabelMap.getOrNull(index) ?: "Hindi alam"
        }
    }

    fun getDistanceLabel(depthValue: Float, language: Language): String {
        val key = when {
            depthValue >= VERY_CLOSE_THRESHOLD -> "very_close"
            depthValue >= CLOSE_THRESHOLD -> "close"
            depthValue >= MEDIUM_THRESHOLD -> "medium"
            else -> "far"
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
