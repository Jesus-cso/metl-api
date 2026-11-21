package com.example.metl

import android.content.Context

object Cache {
    private fun sp(c: Context) = c.getSharedPreferences("metl_cache", Context.MODE_PRIVATE)

    fun guardar(c: Context, clave: String, json: String) {
        sp(c).edit().putString(clave, json).apply()
    }

    fun leer(c: Context, clave: String): String? = sp(c).getString(clave, null)

    fun limpiar(c: Context) = sp(c).edit().clear().apply()
}