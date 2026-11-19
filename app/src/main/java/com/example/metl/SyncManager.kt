package com.example.metl

import android.content.Context

class SyncManager(context: Context) {
    private val repo = ApiCliente(context.applicationContext)

    // Es suspend: llámala desde una corutina (lifecycleScope.launch { ... })
    suspend fun sincronizar() {
        repo.refrescar()   // baja productos y magueyes y los guarda en SQLite
    }
}