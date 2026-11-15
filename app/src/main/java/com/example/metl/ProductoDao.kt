package com.example.metl;
import android.content.ContentValues
import android.content.Context
import org.json.JSONArray

class ProductoDao(ctx: Context) {
    private val helper = DbHelper(ctx)

    fun replaceAll(arr: JSONArray) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("productos", null, null)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val v = ContentValues().apply {
                    put("id", o.getInt("id"))
                    put("nombre", o.getString("nombre"))
                    put("descripcion", o.optString("descripcion"))
                    put("precio", o.getDouble("precio"))
                    put("stock", o.getDouble("stock"))
                    put("proveedor", o.optString("proveedor"))
                    put("municipio", o.optString("municipio"))
                }
                db.insert("productos", null, v)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun listarTexto(): List<String> {
        val out = mutableListOf<String>()
        helper.readableDatabase.rawQuery(
            "SELECT nombre, precio, stock, proveedor, municipio FROM productos ORDER BY nombre", null
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    "${c.getString(0)} · \$${c.getDouble(1)}\n" +
                            "${c.getString(3)} (${c.getString(4)}) · Stock: ${c.getDouble(2)}"
                )
            }
        }
        return out
    }
}