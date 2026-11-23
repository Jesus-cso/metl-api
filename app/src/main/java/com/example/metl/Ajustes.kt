package com.example.metl

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/** Preferencias del dispositivo: ubicación y tema. */
object Ajustes {
    private fun sp(c: Context) =
        c.applicationContext.getSharedPreferences("metl_ajustes", Context.MODE_PRIVATE)

    // ---- Ubicación ----
    fun latitud(c: Context): Double? = sp(c).getString("lat", null)?.toDoubleOrNull()
    fun longitud(c: Context): Double? = sp(c).getString("lon", null)?.toDoubleOrNull()

    fun guardarUbicacion(c: Context, lat: Double?, lon: Double?) {
        sp(c).edit().apply {
            if (lat == null || lon == null) {
                remove("lat"); remove("lon")
            } else {
                putString("lat", lat.toString())
                putString("lon", lon.toString())
            }
        }.apply()
    }

    // ---- Tema: 0 = sistema, 1 = claro, 2 = oscuro ----
    fun tema(c: Context): Int = sp(c).getInt("tema", 0)

    fun guardarTema(c: Context, tema: Int) {
        sp(c).edit().putInt("tema", tema).apply()
        aplicarTema(c)
    }

    fun aplicarTema(c: Context) {
        AppCompatDelegate.setDefaultNightMode(
            when (tema(c)) {
                1 -> AppCompatDelegate.MODE_NIGHT_NO
                2 -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
}