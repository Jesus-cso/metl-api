package com.example.metl

enum class Rol { PRODUCTOR, CLIENTE }

data class Producto(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val presentacion: String,
    val precio: Double,
    val disponibles: Int,
    val lote: String,
    val productor: String
)