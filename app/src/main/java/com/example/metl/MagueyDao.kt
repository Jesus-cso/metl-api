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
                    put("variedad", o.optString("variedad"))
                    put("edad_anios", o.optInt("edadAnios"))
                    put("ubicacion", o.optString("ubicacion"))
                    if (!o.isNull("humedad")) put("humedad", o.getDouble("humedad"))
                    if (!o.isNull("temperatura")) put("temperatura", o.getDouble("temperatura"))
                    put("estado", o.optString("estado"))
                    put("nota", o.optString("nota"))
                }
                db.insert("magueyes", null, v)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
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