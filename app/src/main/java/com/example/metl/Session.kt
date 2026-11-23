package com.example.metl

import android.content.Context
import android.content.SharedPreferences

object Session {
    private fun sp(c: Context): SharedPreferences =
        c.getSharedPreferences("metl_session", Context.MODE_PRIVATE)

    fun guardar(c: Context, token: String, rol: String, nombre: String = "") {
        sp(c).edit()
            .putString("token", token)
            .putString("rol", rol)
            .putString("nombre", nombre)
            .apply()
    }

    fun cambiarNombre(c: Context, nombre: String) =
        sp(c).edit().putString("nombre", nombre).apply()

    fun token(c: Context): String? = sp(c).getString("token", null)
    fun rol(c: Context): String? = sp(c).getString("rol", null)
    fun nombre(c: Context): String = sp(c).getString("nombre", "") ?: ""
    fun cerrar(c: Context) = sp(c).edit().clear().apply()
}