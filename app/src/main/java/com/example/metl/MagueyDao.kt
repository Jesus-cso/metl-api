package com.example.metl

import android.content.ContentValues
import android.content.Context
import org.json.JSONArray

class MagueyDao(ctx: Context) {
    private val helper = DbHelper(ctx)

    fun replaceAll(arr: JSONArray) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("magueyes", null, null)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val v = ContentValues().apply {
                    put("codigo", o.getString("id"))
                    put("variedad", o.texto("variedad"))
                    put("edad_anios", o.optInt("edadAnios"))
                    put("ubicacion", o.texto("ubicacion"))
                    if (!o.isNull("humedad")) put("humedad", o.getDouble("humedad"))
                    if (!o.isNull("temperatura")) put("temperatura", o.getDouble("temperatura"))
                    put("estado", o.texto("estado"))
                    put("nota", o.texto("nota"))
                }
                db.insert("magueyes", null, v)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun listar(lecturas: LecturaDao): List<Maguey> {
        val out = mutableListOf<Maguey>()
        helper.readableDatabase.rawQuery(
            "SELECT codigo, variedad, edad_anios, ubicacion, humedad, temperatura, estado, nota " +
                    "FROM magueyes ORDER BY codigo", null
        ).use { c ->
            while (c.moveToNext()) {
                val codigo = c.getString(0)
                out.add(
                    Maguey(
                        id = codigo,
                        variedad = c.getString(1) ?: "",
                        edadAnios = c.getInt(2),
                        ubicacion = c.getString(3) ?: "",
                        humedad = if (c.isNull(4)) 0 else c.getDouble(4).toInt(),
                        temperatura = if (c.isNull(5)) 0 else c.getDouble(5).toInt(),
                        estado = estadoDe(c.getString(6)),
                        nota = c.getString(7) ?: "",
                        historialHumedad = lecturas.ultimasHumedades(codigo, 9),
                        recomendacion = null
                    )
                )
            }
        }
        return out
    }

    fun listarTexto(): List<String> {
        val out = mutableListOf<String>()
        helper.readableDatabase.rawQuery(
            "SELECT codigo, variedad, humedad, temperatura, estado FROM magueyes ORDER BY codigo", null
        ).use { c ->
            while (c.moveToNext()) {
                val hum = if (c.isNull(2)) "--" else "${c.getDouble(2)}%"
                val temp = if (c.isNull(3)) "--" else "${c.getDouble(3)}°C"
                out.add("${c.getString(0)} · ${c.getString(1)}\nHumedad: $hum  Temp: $temp\nEstado: ${c.getString(4)}")
            }
        }
        return out
    }
}

private fun estadoDe(texto: String?): EstadoMaguey {
    val t = texto.orEmpty().lowercase()
    return when {
        "capar" in t -> EstadoMaguey.CAPAR_PRONTO
        "riego" in t -> EstadoMaguey.RIEGO_BAJO
        else -> EstadoMaguey.EN_DESARROLLO
    }
}