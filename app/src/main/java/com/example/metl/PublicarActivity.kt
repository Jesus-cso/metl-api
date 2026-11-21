package com.example.metl

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
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

class PublicarActivity : AppCompatActivity() {

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

            if (nombre.isEmpty()) { etNombre.error = "Escribe el nombre"; return@setOnClickListener }
            if (precio == null || precio < 0) { etPrecio.error = "Precio no válido"; return@setOnClickListener }
            if (stock == null || stock < 0) { etStock.error = "Existencias no válidas"; return@setOnClickListener }

            btnPublicar.isEnabled = false
            btnPublicar.setText(R.string.publicando)

            lifecycleScope.launch {
                var productoId = -1
                try {
                    productoId = withContext(Dispatchers.IO) {
                        crearProducto(nombre, etDescripcion.text.toString().trim(), precio, stock)
                    }
                    fotoUri?.let { uri ->
                        withContext(Dispatchers.IO) { subirFoto(productoId, uri) }
                    }
                    Toast.makeText(this@PublicarActivity, "Publicado", Toast.LENGTH_SHORT).show()
                    finish()
                } catch (e: Exception) {
                    if (productoId > 0) {
                        // El producto sí quedó creado; solo falló la foto
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

    private fun crearProducto(nombre: String, descripcion: String, precio: Double, stock: Int): Int {
        val cuerpo = JSONObject()
            .put("nombre", nombre)
            .put("descripcion", descripcion)
            .put("precio", precio)
            .put("stock", stock)
            .toString()

        val con = URL("${Config.BASE_URL}/productos").openConnection() as HttpURLConnection
        try {
            con.requestMethod = "POST"
            con.connectTimeout = 10_000
            con.readTimeout = 15_000
            con.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            con.setRequestProperty("Authorization", "Bearer ${Sesion.token(this)}")
            con.doOutput = true
            con.outputStream.use { it.write(cuerpo.toByteArray(Charsets.UTF_8)) }

            val texto = leer(con)
            if (con.responseCode !in 200..299) throw Exception(mensajeError(texto))
            return JSONObject(texto).getInt("id")
        } finally {
            con.disconnect()
        }
    }

    private fun subirFoto(productoId: Int, uri: Uri) {
        val bytes = comprimir(uri)
        val limite = "----metl${System.currentTimeMillis()}"

        val con = URL("${Config.BASE_URL}/productos/$productoId/foto").openConnection() as HttpURLConnection
        try {
            con.requestMethod = "POST"
            con.connectTimeout = 10_000
            con.readTimeout = 30_000
            con.setRequestProperty("Authorization", "Bearer ${Sesion.token(this)}")
            con.setRequestProperty("Content-Type", "multipart/form-data; boundary=$limite")
            con.doOutput = true
            con.outputStream.use { out ->
                out.write("--$limite\r\n".toByteArray())
                out.write("Content-Disposition: form-data; name=\"foto\"; filename=\"foto.jpg\"\r\n".toByteArray())
                out.write("Content-Type: image/jpeg\r\n\r\n".toByteArray())
                out.write(bytes)
                out.write("\r\n--$limite--\r\n".toByteArray())
            }

            val texto = leer(con)
            if (con.responseCode !in 200..299) throw Exception(mensajeError(texto))
        } finally {
            con.disconnect()
        }
    }

    /** Reduce la foto a máx. 1280 px, respeta la rotación y la deja en JPEG (bajo 5 MB). */
    private fun comprimir(uri: Uri): ByteArray {
        val maximo = 1280
        val bitmap: Bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri)) { dec, info, _ ->
            dec.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val mayor = maxOf(info.size.width, info.size.height)
            if (mayor > maximo) {
                val f = maximo.toFloat() / mayor
                dec.setTargetSize((info.size.width * f).toInt(), (info.size.height * f).toInt())
            }
        }

        val salida = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, salida)
        bitmap.recycle()
        val bytes = salida.toByteArray()
        if (bytes.size > 5 * 1024 * 1024) throw Exception("La foto sigue pesando más de 5 MB")
        return bytes
    }

    private fun leer(con: HttpURLConnection): String {
        val flujo = if (con.responseCode in 200..299) con.inputStream else con.errorStream
        return flujo?.bufferedReader()?.use { it.readText() } ?: ""
    }

    private fun mensajeError(texto: String): String =
        runCatching { JSONObject(texto).getString("error") }.getOrDefault("Error del servidor")
}