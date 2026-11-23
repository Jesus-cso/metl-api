package com.example.metl

import android.content.Context

class SyncManager(context: Context) {
    private val repo = ApiRepository(context.applicationContext)

    // Es suspend: llÃ¡mala desde una corutina (lifecycleScope.launch { ... })
    suspend fun sincronizar() {
        repo.refrescar()   // baja productos y magueyes y los guarda en SQLite
    }
}
