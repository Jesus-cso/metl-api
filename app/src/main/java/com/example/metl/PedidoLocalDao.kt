package com.example.metl

import android.content.ContentValues
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class PedidoLocalDao(ctx: Context) {
    private val helper = DbHelper(ctx)

    fun encolar(proveedorId: Int, nota: String?, items: JSONArray): Long {
        val v = ContentValues().apply {
            put("proveedor_id", proveedorId)
            put("nota", nota)
            put("items_json", items.toString())
        }
        return helper.writableDatabase.insert("pedidos_locales", null, v)
    }

    fun pendientes(): List<JSONObject> {
        val out = mutableListOf<JSONObject>()
        helper.readableDatabase.rawQuery(
            "SELECT id, proveedor_id, nota, items_json FROM pedidos_locales WHERE enviado = 0 ORDER BY id", null
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    JSONObject()
                        .put("_local_id", c.getLong(0))
                        .put("proveedor_id", c.getInt(1))
                        .put("nota", c.getString(2))
                        .put("items", JSONArray(c.getString(3)))
                )
            }
        }
        return out
    }

    fun marcarEnviado(localId: Long) {
        val v = ContentValues().apply { put("enviado", 1) }
        helper.writableDatabase.update("pedidos_locales", v, "id = ?", arrayOf(localId.toString()))
    }
}