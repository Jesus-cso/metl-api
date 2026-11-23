package com.example.metl

import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** Todas las pantallas heredan de aqui: tamano de letra elegido y narradora de voz. */
open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val escala = Accesibilidad.escala(newBase)
        if (escala > 0f) {
            val config = Configuration(newBase.resources.configuration)
            config.fontScale = escala
            super.attachBaseContext(newBase.createConfigurationContext(config))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onResume() {
        super.onResume()
        if (Accesibilidad.vozActiva(this) && !talkBackActivo()) {
            val raiz = findViewById<View>(android.R.id.content)
            raiz.postDelayed({
                if (!isFinishing) Voz.hablar(this, resumenPantalla(raiz))
            }, 1200)
        }
    }

    override fun onPause() {
        super.onPause()
        Voz.detener()
    }

    private fun talkBackActivo(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.isEnabled && am.isTouchExplorationEnabled
    }

    /** Junta los textos visibles de la pantalla (maximo ~500 caracteres). */
    private fun resumenPantalla(v: View, sb: StringBuilder = StringBuilder()): String {
        if (v.visibility != View.VISIBLE || sb.length > 500) return sb.toString()
        if (v is TextView) {
            val t = when {
                !v.contentDescription.isNullOrBlank() -> v.contentDescription.toString()
                v.text.isNotBlank() -> v.text.toString()
                !v.hint.isNullOrBlank() -> "Campo: ${v.hint}"
                else -> ""
            }
            if (t.isNotBlank()) sb.append(t).append(". ")
        } else if (v !is ViewGroup && !v.contentDescription.isNullOrBlank()) {
            sb.append(v.contentDescription).append(". ")
        }
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) resumenPantalla(v.getChildAt(i), sb)
        }
        return sb.toString().take(500)
    }
}