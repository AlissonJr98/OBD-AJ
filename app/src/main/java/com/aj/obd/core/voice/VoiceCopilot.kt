package com.aj.obd.core.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceCopilot(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false
    var isEnabled: Boolean = true

    private var lastSpokenTime: Long = 0
    private var lastMessage: String = ""

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("pt", "BR"))
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isInitialized = true
                tts?.setPitch(1.05f)
                tts?.setSpeechRate(1.02f)
            }
        }
    }

    /**
     * Fala a mensagem caso a voz esteja ativada e com debounce de segurança
     */
    fun speak(message: String, priority: Boolean = false) {
        if (!isEnabled || !isInitialized || tts == null) return

        val now = System.currentTimeMillis()
        // Evita repetir a mesma frase em menos de 10 segundos, exceto alta prioridade
        if (!priority && message == lastMessage && (now - lastSpokenTime) < 10000) {
            return
        }

        // Intervalo mínimo entre qualquer fala (3 segundos)
        if (!priority && (now - lastSpokenTime) < 3000) {
            return
        }

        lastSpokenTime = now
        lastMessage = message

        val queueMode = if (priority) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts?.speak(message, queueMode, null, "obd_voice_${System.currentTimeMillis()}")
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (_: Exception) {}
    }
}
