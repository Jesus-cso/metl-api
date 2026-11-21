package com.example.metl

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray

class LecturaDao(ctx: Context) {
    private val helper = DbHelper(ctx)

    fun insertar(codigo: String, arr: JSONArray) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val v = ContentValues().apply {
                    put("maguey_codigo", codigo)
                    if (!o.isNull("humedad")) put("humedad", o.getDouble("humedad"))
                    if (!o.isNull("temperatura")) put("temperatura", o.getDouble("temperatura"))
                    put("creado_en", o.getString("creado_en"))
                }
                db.insertWithOnConflict("lecturas", null, v, SQLiteDatabase.CONFLICT_IGNORE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // Las últimas n humedades en orden cronológico (de la más vieja a la más nueva)
    fun ultimasHumedades(codigo: String, n: Int): List<Int> {
        val out = mutableListOf<Int>()
        helper.readableDatabase.rawQuery(
            "SELECT humedad FROM lecturas WHERE maguey_codigo = ? AND humedad IS NOT NULL " +
                    "ORDER BY creado_en DESC LIMIT $n", arrayOf(codigo)
        ).use { c ->
            while (c.moveToNext()) out.add(c.getDouble(0).toInt())
        }
        return out.reversed()
    }
}