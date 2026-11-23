package com.example.metl

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Preferencias de accesibilidad guardadas en el telefono. */
object Accesibilidad {
    private const val PREFS = "accesibilidad"
    private const val K_ESCALA = "escala"
    private const val K_VIBRAR = "vibrar"
    private const val K_VOZ = "voz"

    private fun p(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 0f = usar el tamano de letra del sistema. */
    fun escala(ctx: Context): Float = p(ctx).getFloat(K_ESCALA, 0f)
    fun guardarEscala(ctx: Context, valor: Float) { p(ctx).edit().putFloat(K_ESCALA, valor).apply() }

    fun vibrarActivo(ctx: Context): Boolean = p(ctx).getBoolean(K_VIBRAR, true)
    fun guardarVibrar(ctx: Context, activo: Boolean) { p(ctx).edit().putBoolean(K_VIBRAR, activo).apply() }

    fun vozActiva(ctx: Context): Boolean = p(ctx).getBoolean(K_VOZ, false)
    fun guardarVoz(ctx: Context, activo: Boolean) { p(ctx).edit().putBoolean(K_VOZ, activo).apply() }

    /** Vibracion corta para exito, doble para error. Sirve a quien no oye. */
    fun vibrar(ctx: Context, error: Boolean) {
        if (!vibrarActivo(ctx)) return
        try {
            val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
                (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (v == null || !v.hasVibrator()) return
            val patron = if (error) longArrayOf(0, 120, 80, 120) else longArrayOf(0, 60)
            v.vibrate(VibrationEffect.createWaveform(patron, -1))
        } catch (_: Exception) {
        }
    }
}