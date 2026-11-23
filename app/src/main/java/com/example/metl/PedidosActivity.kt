package com.example.metl

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Locale

class PedidosActivity : AppCompatActivity() {

    private val repo by lazy { ApiRepository(applicationContext) }
    private val esProveedor by lazy { Session.rol(this) == "proveedor" }
    private lateinit var lista: LinearLayout
    private lateinit var vacio: TextView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun texto(t: String, size: Float, colorRes: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = t
            textSize = size
            setTextColor(ContextCompat.getColor(this@PedidosActivity, colorRes))
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            fitsSystemWindows = true
            setBackgroundColor(ContextCompat.getColor(this@PedidosActivity, R.color.app_bg))
        }

        val encabezado = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(8))
        }
        val titulo = texto(
            if (esProveedor) "Pedidos recibidos" else "Mis pedidos",
            26f, R.color.ink, true
        ).apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        val volver = texto("Volver", 14f, R.color.ink, true).apply {
            setBackgroundResource(R.drawable.bg_card)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            setOnClickListener { finish() }
        }
        encabezado.addView(titulo)
        encabezado.addView(volver)

        vacio = texto("Cargando…", 14f, R.color.muted).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, 0)
        }
        lista = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val contenido = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), 0, dp(20), dp(24))
            addView(vacio)
            addView(lista)
        }
        val scroll = ScrollView(this).apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
            addView(contenido)
        }

        raiz.addView(encabezado)
        raiz.addView(scroll)
        setContentView(raiz)
    }

    override fun onResume() {
        super.onResume()
        cargar()
    }

    private fun cargar() {
        lifecycleScope.launch {
            val r = if (esProveedor) repo.pedidosRecibidos() else repo.misPedidos()
            r.onSuccess { pintar(it) }
                .onFailure {
                    lista.removeAllViews()
                    vacio.text = it.message ?: "Error al cargar"
                    vacio.visibility = View.VISIBLE
                }
        }
    }

    private fun pintar(pedidos: List<PedidoApi>) {
        lista.removeAllViews()
        vacio.text = if (esProveedor) "Aún no tienes pedidos" else "Aún no has hecho pedidos"
        vacio.visibility = if (pedidos.isEmpty()) View.VISIBLE else View.GONE
        pedidos.forEach { lista.addView(tarjeta(it)) }
    }

    private fun etiqueta(estado: String) = when (estado) {
        "pendiente" -> "Pendiente"
        "aceptado" -> "Aceptado"
        "rechazado" -> "Rechazado"
        "entregado" -> "Entregado"
        "cancelado" -> "Cancelado"
        else -> estado
    }

    private fun tarjeta(p: PedidoApi): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
        }
        card.addView(texto("Pedido #${p.id} · ${etiqueta(p.estado)}", 16f, R.color.ink, true))
        card.addView(
            texto(
                (if (esProveedor) "Cliente: " else "Proveedor: ") + p.contraparte,
                13f, R.color.muted
            )
        )

        var total = 0.0
        p.items.forEach { i ->
            val sub = i.cantidad * i.precioUnit
            total += sub
            card.addView(
                texto(
                    String.format(Locale.US, "%.0f × %s — $%.2f", i.cantidad, i.nombre, sub),
                    14f, R.color.ink
                ).apply { setPadding(0, dp(4), 0, 0) }
            )
        }
        card.addView(
            texto(String.format(Locale.US, "Total: $%.2f", total), 15f, R.color.ink, true)
                .apply { setPadding(0, dp(8), 0, 0) }
        )
        if (p.nota.isNotBlank()) {
            card.addView(texto("Nota: ${p.nota}", 13f, R.color.muted).apply { setPadding(0, dp(4), 0, 0) })
        }

        if (esProveedor) {
            val fila = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(10), 0, 0)
            }
            fun boton(t: String, nuevo: String) = Button(this).apply {
                text = t
                setOnClickListener { cambiar(p.id, nuevo) }
            }
            when (p.estado) {
                "pendiente" -> {
                    fila.addView(boton("Aceptar", "aceptado"))
                    fila.addView(boton("Rechazar", "rechazado"))
                }
                "aceptado" -> fila.addView(boton("Marcar entregado", "entregado"))
            }
            if (fila.childCount > 0) card.addView(fila)
        }
        if (!esProveedor && p.estado == "pendiente") {
            card.addView(Button(this).apply {
                text = "Cancelar pedido"
                setOnClickListener { confirmarCancelar(p.id) }
            })
        }
        return card
    }

    private fun confirmarCancelar(id: Int) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Cancelar pedido")
            .setMessage("\u00bfSeguro que quieres cancelar el pedido #$id?")
            .setPositiveButton("S\u00ed, cancelar") { _, _ ->
                lifecycleScope.launch {
                    repo.cancelarPedido(id)
                        .onSuccess { cargar() }
                        .onFailure { Toast.makeText(this@PedidosActivity, it.message ?: "Error", Toast.LENGTH_LONG).show() }
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
    private fun cambiar(id: Int, estado: String) {
        lifecycleScope.launch {
            repo.cambiarEstado(id, estado)
                .onSuccess { cargar() }
                .onFailure { Toast.makeText(this@PedidosActivity, it.message ?: "Error", Toast.LENGTH_LONG).show() }
        }
    }
}
