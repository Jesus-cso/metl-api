package com.example.metl

data class PedidoItemApi(val nombre: String, val cantidad: Double, val precioUnit: Double)

data class PedidoApi(
    val id: Int,
    val estado: String,          // pendiente | aceptado | rechazado | entregado
    val nota: String,
    val contraparte: String,     // proveedor (si eres cliente) o cliente (si eres proveedor)
    val items: List<PedidoItemApi>
)
