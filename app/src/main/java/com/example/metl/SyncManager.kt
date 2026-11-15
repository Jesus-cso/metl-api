package com.example.metl

import android.content.Context

class SyncManager(context: Context) {
    private val ctx = context.applicationContext
    private val api = ApiClient(ctx)

    // Llamar desde un hilo secundario
    fun sincronizar() {
        if (Session.rol(ctx) == "proveedor") syncProveedor() else syncCliente()
    }

    private fun syncProveedor() {
        val magueyes = api.getArray("/proveedor/magueyes")
        MagueyDao(ctx).replaceAll(magueyes)
        val lecturas = LecturaDao(ctx)
        for (i in 0 until magueyes.length()) {
            val codigo = magueyes.getJSONObject(i).getString("id")
            lecturas.insertar(codigo, api.getArray("/sensores/magueyes/$codigo/lecturas?limite=50"))
        }
    }

    private fun syncCliente() {
        val cola = PedidoLocalDao(ctx)
        for (p in cola.pendientes()) {
            val localId = p.getLong("_local_id")
            p.remove("_local_id")
            api.postObject("/pedidos", p)
            cola.marcarEnviado(localId)
        }
        ProductoDao(ctx).replaceAll(api.getArray("/productos/publicos"))
    }
}