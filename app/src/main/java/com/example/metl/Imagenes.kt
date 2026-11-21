package com.example.metl

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.ImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object Imagenes {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Usa 1/8 de la memoria disponible para la caché
    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount / 1024
    }

    fun cargar(vista: ImageView, url: String?) {
        vista.tag = url
        vista.setImageResource(android.R.color.darker_gray)
        if (url == null) return

        cache.get(url)?.let {
            vista.setImageBitmap(it)
            return
        }

        scope.launch {
            val bmp = withContext(Dispatchers.IO) { descargar(url) }
            if (bmp != null) {
                cache.put(url, bmp)
                // Solo se pinta si la vista no fue reciclada para otra publicación
                if (vista.tag == url) vista.setImageBitmap(bmp)
            }
        }
    }

    private fun descargar(url: String): Bitmap? = runCatching {
        val con = URL(url).openConnection() as HttpURLConnection
        try {
            con.connectTimeout = 10_000
            con.readTimeout = 15_000
            if (con.responseCode !in 200..299) return null
            val bytes = con.inputStream.use { it.readBytes() }

            // Reduce si la imagen es muy grande, para no gastar memoria
            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, limites)
            var muestra = 1
            while (maxOf(limites.outWidth, limites.outHeight) / muestra > 1600) muestra *= 2

            val opciones = BitmapFactory.Options().apply { inSampleSize = muestra }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opciones)
        } finally {
            con.disconnect()
        }
    }.getOrNull()
}