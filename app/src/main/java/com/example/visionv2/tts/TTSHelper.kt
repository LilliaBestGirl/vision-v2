package com.example.visionv2.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import com.example.visionv2.settings.LanguagePreferences
import com.example.visionv2.utils.Translations
import java.util.Locale

class TTSHelper(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var lastAudioTime: Long = 0
    private val cueCooldown = 5000L
    private var currentLangCode: String = "en-US"

    init {
        // Load saved language before initializing TTS
        currentLangCode = LanguagePreferences.loadLanguage(context)

        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                setLanguageFromCode(currentLangCode)
                tts?.speak("Welcome to VISION", TextToSpeech.QUEUE_FLUSH, null, null)
            } else {
                Log.e("TTSHelper", "TTS Initialization Error")
            }
        }
    }

    private fun setLanguageFromCode(code: String) {
        val parts = code.split("-")
        if (parts.size == 2) {
            val locale = Locale(parts[0], parts[1])
            tts?.language = locale
            Log.d("TTSHelper", "Loaded language: $code")
        }
    }

    fun changeLanguage(language: String, country: String) {
        val locale = Locale(language, country)
        tts?.language = locale
        currentLangCode = "$language-$country"
        LanguagePreferences.saveLanguage(context, currentLangCode)

        Toast.makeText(
            context,
            "Language changed to ${locale.displayLanguage}",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun speak(text: String, bypassCooldown: Boolean = false) {
        val currentTime = System.currentTimeMillis()
        if (bypassCooldown || currentTime - lastAudioTime >= cueCooldown) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            lastAudioTime = currentTime
        }
    }

    fun speakDetection(index: Int, depthValue: Float) {
        val lang = Translations.Language.fromCode(currentLangCode)
        val label = Translations.getLabel(index, lang)
        val distance = Translations.getDistanceLabel(depthValue, lang)
        val sentence = Translations.getSentence(label, distance, lang)
        speak(sentence)
    }

    fun shutdown() {
        tts?.shutdown()
    }
}
