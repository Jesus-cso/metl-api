package com.example.metl

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.Locale

class SensoresActivity : BaseActivity() {

    private companion object {
        const val TAG = "Sensores"
        const val REFRESCO_MS = 15_000L
    }

    private lateinit var contenedor: LinearLayout
    private lateinit var tvEstado: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensores)

        contenedor = findViewById(R.id.contenedor)
        tvEstado = findViewById(R.id.tvSensoresEstado)

        findViewById<TextView>(R.id.btnVolver).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnRegistrar).setOnClickListener { pedirNombre() }

        // Se actualiza solo mientras la pantalla está visible
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    cargar()
                    delay(REFRESCO_MS)
                }
            }
        }
    }

    // ------------------------------------------------------------------ LISTA

    private suspend fun cargar() {
        val token = Session.token(this).orEmpty()
        try {
            val (codigo, texto) = withContext(Dispatchers.IO) {
                peticion("GET", "/sensores/proveedor", token, null)
            }
            if (codigo !in 200..299) {
                tvEstado.text = "HTTP $codigo: ${mensajeError(texto)}"
                return
            }
            pintar(JSONArray(texto))
        } catch (e: Exception) {
            Log.e(TAG, "cargar", e)
            tvEstado.text = "Sin conexión: ${e.message ?: e.javaClass.simpleName}"
        }
    }

    private fun pintar(lista: JSONArray) {
        contenedor.removeAllViews()
        tvEstado.text = if (lista.length() == 0) {
            "Aún no tienes sensores. Registra el primero."
        } else {
            "Se actualiza cada 15 segundos."
        }

        for (i in 0 until lista.length()) {
            val s = lista.getJSONObject(i)
            val v = LayoutInflater.from(this).inflate(R.layout.item_sensor, contenedor, false)

            v.findViewById<TextView>(R.id.tvSensorNombre).text = s.optString("nombre")

            val tvTemp = v.findViewById<TextView>(R.id.tvTemp)
            val tvHum = v.findViewById<TextView>(R.id.tvHum)
            val tvUltima = v.findViewById<TextView>(R.id.tvUltima)

            if (s.isNull("temperatura")) {
                tvTemp.text = "— °C"
                tvHum.text = "— %"
                tvUltima.text = "Sin lecturas todavía"
            } else {
                tvTemp.text = String.format(Locale.US, "%.1f °C", s.optDouble("temperatura"))
                tvHum.text = String.format(Locale.US, "%.1f %%", s.optDouble("humedad"))
                tvUltima.text = "Última lectura: ${hace(s.optString("ultima_lectura"))}"
            }
            contenedor.addView(v)
        }
    }

    private fun hace(iso: String): String = runCatching {
        val min = Duration.between(Instant.parse(iso), Instant.now()).toMinutes()
        when {
            min < 1 -> "hace menos de 1 min"
            min < 60 -> "hace $min min"
            min < 1440 -> "hace ${min / 60} h"
            else -> "hace ${min / 1440} días"
        }
    }.getOrDefault(iso)

    // --------------------------------------------------------------- REGISTRAR

    private fun pedirNombre() {
        val et = EditText(this).apply {
            hint = "Ej. Invernadero 1"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setSingleLine()
        }
        val caja = LinearLayout(this).apply {
            setPadding(48, 16, 48, 0)
            addView(et, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        AlertDialog.Builder(this)
            .setTitle("Registrar sensor")
            .setMessage("Ponle un nombre para reconocerlo.")
            .setView(caja)
            .setPositiveButton("Registrar") { _, _ ->
                val nombre = et.text.toString().trim()
                if (nombre.length < 2) {
                    Toast.makeText(this, "Escribe un nombre (mínimo 2 letras)", Toast.LENGTH_SHORT).show()
                } else {
                    registrar(nombre)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun registrar(nombre: String) {
        val token = Session.token(this).orEmpty()
        val lat = Ajustes.latitud(this)
        val lon = Ajustes.longitud(this)

        lifecycleScope.launch {
            try {
                val cuerpo = JSONObject().put("nombre", nombre)
                if (lat != null && lon != null) {
                    cuerpo.put("latitud", lat).put("longitud", lon)
                }

                val (codigo, texto) = withContext(Dispatchers.IO) {
                    peticion("POST", "/sensores", token, cuerpo.toString())
                }
                if (codigo !in 200..299) {
                    Toast.makeText(
                        this@SensoresActivity,
                        "HTTP $codigo: ${mensajeError(texto)}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                val clave = JSONObject(texto).getString("api_key")
                mostrarClave(nombre, clave)
                cargar()
            } catch (e: Exception) {
                Log.e(TAG, "registrar", e)
                Toast.makeText(
                    this@SensoresActivity,
                    "Error: ${e.message ?: e.javaClass.simpleName}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun mostrarClave(nombre: String, clave: String) {
        val tv = TextView(this).apply {
            text = clave
            textSize = 14f
            setTextIsSelectable(true)
            setPadding(48, 24, 48, 0)
        }

        AlertDialog.Builder(this)
            .setTitle("Sensor \"$nombre\" creado")
            .setMessage("Esta clave se muestra UNA sola vez. Cópiala y pégala en el código del ESP32 (API_KEY).")
            .setView(tv)
            .setPositiveButton("Copiar") { _, _ ->
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("api_key", clave))
                Toast.makeText(this, "Clave copiada", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cerrar", null)
            .setCancelable(false)
            .show()
    }

    // ------------------------------------------------------------------- HTTP

    private fun peticion(metodo: String, ruta: String, token: String, cuerpo: String?): Pair<Int, String> {
        val con = URL("${Config.baseUrl}/${ruta.trimStart('/')}").openConnection() as HttpURLConnection
        try {
            con.requestMethod = metodo
            con.connectTimeout = 10_000
            con.readTimeout = 15_000
            con.setRequestProperty("Authorization", "Bearer $token")
            if (cuerpo != null) {
                con.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                con.doOutput = true
                con.outputStream.use { it.write(cuerpo.toByteArray(Charsets.UTF_8)) }
            }
            val codigo = con.responseCode
            val flujo = if (codigo in 200..299) con.inputStream else con.errorStream
            val texto = flujo?.bufferedReader()?.use { it.readText() } ?: ""
            Log.d(TAG, "$metodo $ruta -> $codigo")
            return codigo to texto
        } finally {
            con.disconnect()
        }
    }

    private fun mensajeError(texto: String): String {
        if (texto.isBlank()) return "sin respuesta del servidor"
        return runCatching { JSONObject(texto).getString("error") }
            .getOrDefault(texto.take(200))
    }
}