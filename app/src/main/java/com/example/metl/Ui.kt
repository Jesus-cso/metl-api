package com.example.metl

import android.widget.TextView

fun TextView.aplicarChip(texto: String, tipo: ChipTipo) {
    text = texto
    val fondo: Int
    val color: Int
    when (tipo) {
        ChipTipo.OK -> {
            fondo = R.drawable.chip_ok
            color = R.color.ok_fg
        }
        ChipTipo.WARN -> {
            fondo = R.drawable.chip_warn
            color = R.color.warn_fg
        }
        ChipTipo.BAD -> {
            fondo = R.drawable.chip_bad
            color = R.color.bad_fg
        }
    }
    setBackgroundResource(fondo)
    setTextColor(context.getColor(color))
}