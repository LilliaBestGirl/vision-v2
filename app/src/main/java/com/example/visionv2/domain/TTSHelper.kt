package com.example.visionv2.domain

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log

class TTSHelper(context: Context) {
    private var tts: TextToSpeech? = null
    private var lastAudioTime: Long = 0
    private val cueCooldown = 5000L

    init {
        tts = TextToSpeech(context) { status ->
            if (status != TextToSpeech.SUCCESS) {
                Log.e("TTSHelper", "TTS Initialization Error")
            }
        }
    }

    fun speak(text: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAudioTime >= cueCooldown) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            lastAudioTime = currentTime
        }
    }

    fun shutdown() {
        tts?.shutdown()
    }
}