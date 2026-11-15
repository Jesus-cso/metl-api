package com.example.metl

import android.content.Context

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ApiClient(context: Context) {
    private val ctx = context.applicationContext

    fun login(correo: String, password: String) {
        val body = JSONObject().put("correo", correo).put("password", password)
        val r = JSONObject(request("POST", "/auth/login", body, false))
        Session.guardar(ctx, r.getString("token"), r.getJSONObject("usuario").getString("rol"))
    }

    fun getArray(ruta: String): JSONArray = JSONArray(request("GET", ruta, null, true))

    fun postObject(ruta: String, body: JSONObject): JSONObject =
        JSONObject(request("POST", ruta, body, true))

    private fun request(metodo: String, ruta: String, body: JSONObject?, conToken: Boolean): String {
        val con = URL(BASE_URL + ruta).openConnection() as HttpURLConnection
        try {
            con.requestMethod = metodo
            con.connectTimeout = 8000
            con.readTimeout = 8000
            con.setRequestProperty("Accept", "application/json")
            if (conToken) {
                Session.token(ctx)?.let { con.setRequestProperty("Authorization", "Bearer $it") }
            }
            if (body != null) {
                con.doOutput = true
                con.setRequestProperty("Content-Type", "application/json")
                con.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = con.responseCode
            val stream = if (code >= 400) con.errorStream else con.inputStream
            val texto = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            if (code >= 400) {
                val msg = try {
                    JSONObject(texto).optString("error", texto)
                } catch (e: JSONException) {
                    texto
                }
                throw IOException(msg)   // la API responde {"error": "..."}
            }
            return texto
        } finally {
            con.disconnect()
        }
    }

    companion object {
        // Emulador: 10.0.2.2 | Celular real: http://IP_DE_TU_PC:3000
        const val BASE_URL = "http://10.0.2.2:3000"
    }
}