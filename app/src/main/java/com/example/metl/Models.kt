package com.example.metl
enum class ChipTipo { OK, WARN, BAD }

enum class EstadoMaguey(val label: String, val chip: ChipTipo) {
    CAPAR_PRONTO("Capar pronto", ChipTipo.OK),
    RIEGO_BAJO("Riego bajo", ChipTipo.BAD),
    EN_DESARROLLO("En desarrollo", ChipTipo.WARN)
}

enum class EstadoPedido(val label: String, val chip: ChipTipo) {
    CONFIRMADO("Confirmado", ChipTipo.OK),
    POR_ACEPTAR("Por aceptar", ChipTipo.WARN)
}

data class Recomendacion(val titulo: String, val confianza: Int, val detalle: String)

data class Maguey(
    val id: String,
    val variedad: String,
    val edadAnios: Int,
    val ubicacion: String,
    val humedad: Int,
    val temperatura: Int,
    val estado: EstadoMaguey,
    val nota: String,
    val historialHumedad: List<Int>,
    val recomendacion: Recomendacion?
)

data class Resumen(
    val parcela: String,
    val usuario: String,
    val totalSensores: Int,
    val listosCapar: Int,
    val alertas: Int,
    val pedidosAbiertos: String
)

data class Paso(val titulo: String, val detalle: String)
data class Pedido(val cliente: String, val detalle: String, val estado: EstadoPedido)
data class Lote(val codigo: String, val pasos: List<Paso>, val pedidos: List<Pedido>)