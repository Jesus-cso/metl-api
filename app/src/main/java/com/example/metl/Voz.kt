package com.example.metl

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Narradora: lee textos en voz alta con el motor de voz del telefono. */
object Voz {
    private var tts: TextToSpeech? = null
    private var listo = false
    private var pendiente: String? = null

    private fun iniciar(ctx: Context) {
        if (tts != null) return
        tts = TextToSpeech(ctx.applicationContext) { estado ->
            if (estado == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(Locale("es", "MX"))
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale("es"))
                }
                listo = true
                pendiente?.let {
                    tts?.speak(it, TextToSpeech.QUEUE_FLUSH, null, "metl")
                    pendiente = null
                }
            }
        }
    }

    fun hablar(ctx: Context, texto: String) {
        if (texto.isBlank()) return
        iniciar(ctx)
        if (listo) tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "metl") else pendiente = texto
    }

    fun detener() {
        tts?.stop()
    }
}