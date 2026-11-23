package com.example.metl

import android.content.Context
import android.content.SharedPreferences

/** Dirección del servidor, editable desde Ajustes. */
object Config {
    const val DEFAULT_URL = "http://192.168.0.104:3100"   // emulador -> PC
    private const val PREFS = "config"
    private const val KEY = "base_url"

    @Volatile private var url: String = DEFAULT_URL
    private var prefs: SharedPreferences? = null

    fun init(ctx: Context) {
        val p = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        url = p.getString(KEY, DEFAULT_URL) ?: DEFAULT_URL
    }

    val baseUrl: String get() = url

    fun guardar(nueva: String) {
        val limpia = nueva.trim().trimEnd('/')
        url = limpia
        prefs?.edit()?.putString(KEY, limpia)?.apply()
    }

    fun restablecer() = guardar(DEFAULT_URL)
}