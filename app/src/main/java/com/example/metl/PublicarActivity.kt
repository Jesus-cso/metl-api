package com.example.metl

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class PublicarActivity : BaseActivity() {

    private companion object {
        const val TAG = "Publicar"
    }

    private var fotoUri: Uri? = null
    private lateinit var ivVistaPrevia: ImageView

    private val elegirFoto =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                fotoUri = uri
                ivVistaPrevia.setImageURI(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_publicar)

        ivVistaPrevia = findViewById(R.id.ivVistaPrevia)

        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etDescripcion = findViewById<EditText>(R.id.etDescripcion)
        val etPrecio = findViewById<EditText>(R.id.etPrecio)
        val etStock = findViewById<EditText>(R.id.etStock)
        val btnPublicar = findViewById<Button>(R.id.btnPublicar)

        findViewById<Button>(R.id.btnElegirFoto).setOnClickListener {
            elegirFoto.launch("image/*")
        }

        btnPublicar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val precio = etPrecio.text.toString().trim().replace(',', '.').toDoubleOrNull()
            val stock = etStock.text.toString().trim().toIntOrNull()

            if (nombre.isEmpty()) {
                etNombre.error = "Escribe el nombre"
                return@setOnClickListener
            }
            if (precio == null || precio < 0) {
                etPrecio.error = "Precio no válido"
                return@setOnClickListener
            }
            if (stock == null || stock < 0) {
                etStock.error = "Existencias no válidas"
                return@setOnClickListener
            }

            val token = Session.token(this).orEmpty()
            if (token.isEmpty()) {
                Toast.makeText(
                    this,
                    "No hay sesión iniciada. Cierra sesión y vuelve a entrar.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            val descripcion = etDescripcion.text.toString().trim()

            btnPublicar.isEnabled = false
            btnPublicar.setText(R.string.publicando)

            lifecycleScope.launch {
                var creado = false
                var productoId = -1

                try {
                    productoId = withContext(Dispatchers.IO) {
                        crearProducto(token, nombre, descripcion, precio, stock)
                    }
                    creado = true

                    val uri = fotoUri
                    if (uri != null) {
                        if (productoId <= 0) {
                            throw Exception("El servidor no devolvió el id del producto")
                        }
                        withContext(Dispatchers.IO) {
                            subirFoto(token, productoId, uri)
                        }
                    }

                    Toast.makeText(this@PublicarActivity, "Publicado", Toast.LENGTH_SHORT).show()
                    finish()

                } catch (e: Exception) {
                    Log.e(TAG, "Fallo al publicar", e)

                    if (creado) {
                        // El producto ya existe: no se debe volver a crear
                        Toast.makeText(
                            this@PublicarActivity,
                            "Se publicó sin foto: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    } else {
                        Toast.makeText(
                            this@PublicarActivity,
                            e.message ?: "Error al publicar",
                            Toast.LENGTH_LONG
                        ).show()
                        btnPublicar.isEnabled = true
                        btnPublicar.setText(R.string.publicar)
                    }
                }
            }
        }
    }

    /** Crea el producto y devuelve su id (o -1 si el servidor no lo envía). */
    private fun crearProducto(
        token: String,
        nombre: String,
        descripcion: String,
        precio: Double,
        stock: Int
    ): Int {
        val cuerpo = JSONObject()
            .put("nombre", nombre)
            .put("descripcion", descripcion)
            .put("precio", precio)
            .put("stock", stock)
            .toString()

        val con = URL("${Config.baseUrl}/productos").openConnection() as HttpURLConnection

        try {
            con.requestMethod = "POST"
            con.connectTimeout = 10_000
            con.readTimeout = 15_000
            con.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            con.setRequestProperty("Authorization", "Bearer $token")
            con.doOutput = true

            con.outputStream.use { it.write(cuerpo.toByteArray(Charsets.UTF_8)) }

            val codigo = con.responseCode
            val texto = leer(con)
            Log.d(TAG, "POST /productos -> $codigo: $texto")

            if (codigo !in 200..299) {
                throw Exception("HTTP $codigo: ${mensajeError(texto)}")
            }

            return extraerId(texto)
        } finally {
            con.disconnect()
        }
    }

    private fun subirFoto(token: String, productoId: Int, uri: Uri) {
        val bytes = comprimir(uri)
        val limite = "----metl${System.currentTimeMillis()}"

        val con = URL("${Config.baseUrl}/productos/$productoId/foto")
            .openConnection() as HttpURLConnection

        try {
            con.requestMethod = "POST"
            con.connectTimeout = 10_000
            con.readTimeout = 30_000
            con.setRequestProperty("Authorization", "Bearer $token")
            con.setRequestProperty("Content-Type", "multipart/form-data; boundary=$limite")
            con.doOutput = true

            con.outputStream.use { out ->
                out.write("--$limite\r\n".toByteArray())
                out.write(
                    "Content-Disposition: form-data; name=\"foto\"; filename=\"foto.jpg\"\r\n"
                        .toByteArray()
                )
                out.write("Content-Type: image/jpeg\r\n\r\n".toByteArray())
                out.write(bytes)
                out.write("\r\n--$limite--\r\n".toByteArray())
            }

            val codigo = con.responseCode
            val texto = leer(con)
            Log.d(TAG, "POST /productos/$productoId/foto -> $codigo: $texto")

            if (codigo !in 200..299) {
                throw Exception("HTTP $codigo: ${mensajeError(texto)}")
            }
        } finally {
            con.disconnect()
        }
    }

    private fun comprimir(uri: Uri): ByteArray {
        val maximo = 1280

        val bitmap: Bitmap = ImageDecoder.decodeBitmap(
            ImageDecoder.createSource(contentResolver, uri)
        ) { dec, info, _ ->
            dec.allocator = ImageDecoder.ALLOCATOR_SOFTWARE

            val mayor = maxOf(info.size.width, info.size.height)
            if (mayor > maximo) {
                val f = maximo.toFloat() / mayor
                dec.setTargetSize(
                    (info.size.width * f).toInt(),
                    (info.size.height * f).toInt()
                )
            }
        }

        val salida = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, salida)
        bitmap.recycle()

        val bytes = salida.toByteArray()
        if (bytes.size > 5 * 1024 * 1024) {
            throw Exception("La foto sigue pesando más de 5 MB")
        }
        return bytes
    }

    private fun leer(con: HttpURLConnection): String {
        val flujo = if (con.responseCode in 200..299) con.inputStream else con.errorStream
        return flujo?.bufferedReader()?.use { it.readText() } ?: ""
    }

    /** Acepta {"id":5}, {"insertId":5} o {"producto":{"id":5}}. */
    private fun extraerId(texto: String): Int = runCatching {
        val j = JSONObject(texto)
        when {
            j.has("id") -> j.getInt("id")
            j.has("insertId") -> j.getInt("insertId")
            j.has("producto") -> j.getJSONObject("producto").getInt("id")
            else -> -1
        }
    }.getOrDefault(-1)

    /** Muestra el error del servidor, o el texto crudo si no es JSON. */
    private fun mensajeError(texto: String): String {
        if (texto.isBlank()) return "sin respuesta del servidor"
        return runCatching {
            val j = JSONObject(texto)
            when {
                j.has("error") -> j.getString("error")
                j.has("message") -> j.getString("message")
                else -> texto.take(200)
            }
        }.getOrDefault(texto.take(200))
    }
}