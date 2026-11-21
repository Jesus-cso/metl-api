package com.example.metl

import org.json.JSONObject

// optString de Android devuelve el texto "null" cuando el JSON trae null.
fun JSONObject.texto(clave: String): String =
    if (isNull(clave)) "" else optString(clave)