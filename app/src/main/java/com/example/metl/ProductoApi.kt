package com.example.metl

data class ProductoApi(
    val id: Int,
    val nombre: String,
    val descripcion: String,
    val precio: Double,
    val stock: Double,
    val activo: Boolean
)
