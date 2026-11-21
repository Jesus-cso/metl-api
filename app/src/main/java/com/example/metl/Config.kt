package com.example.metl

import android.content.Context

object Config {
    const val BASE_URL = "http://192.168.0.104:3000"
}

object Sesion {
    private const val PREFS = "metl"
    private const val CLAVE_TOKEN = "token"

    fun token(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CLAVE_TOKEN, "") ?: ""
}