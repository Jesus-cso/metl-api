package com.example.metl

import android.app.Activity
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar

/** Aviso escrito en pantalla (no desaparece si es importante) y con vibracion. */
fun Activity.avisar(texto: String, importante: Boolean = false, error: Boolean = false) {
    Accesibilidad.vibrar(this, error)
    if (Accesibilidad.vozActiva(this)) Voz.hablar(this, texto)
    val raiz = findViewById<View>(android.R.id.content)
    val s = Snackbar.make(
        raiz, texto,
        if (importante) Snackbar.LENGTH_INDEFINITE else Snackbar.LENGTH_LONG
    )
    if (importante) s.setAction("Cerrar") { s.dismiss() }
    s.show()
}

/** Pide confirmacion antes de una accion que no se puede deshacer. */
fun Activity.confirmar(titulo: String, mensaje: String, textoOk: String, alAceptar: () -> Unit) {
    AlertDialog.Builder(this)
        .setTitle(titulo)
        .setMessage(mensaje)
        .setPositiveButton(textoOk) { _, _ -> alAceptar() }
        .setNegativeButton("No, volver", null)
        .show()
}

/** Convierte errores tecnicos en frases que dicen que hacer. */
fun mensajeAmigable(error: Throwable): String {
    val t = (error.message ?: "").lowercase()
    return when {
        "401" in t || "token" in t || "sesion" in t || "sesión" in t ->
            "Tu sesión terminó. Vuelve a iniciar sesión."
        "resolve host" in t || "failed to connect" in t || "timeout" in t ||
            "timed out" in t || "conexión" in t || "conexion" in t || "network" in t ->
            "No hay conexión con el servidor. Revisa tu Wi-Fi."
        "stock" in t || "suficiente" in t ->
            "No hay suficiente producto. Pide una cantidad menor."
        else -> "No se pudo completar. Intenta de nuevo."
    }
}