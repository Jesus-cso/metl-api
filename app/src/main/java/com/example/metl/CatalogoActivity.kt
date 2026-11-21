package com.example.metl

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Locale

class CatalogoActivity : AppCompatActivity() {

    private val repo by lazy { ApiRepository(applicationContext) }
    private val proveedorId by lazy { intent.getIntExtra("proveedor_id", -1) }
    private val cantidades = mutableMapOf<Int, Int>()
    private var productos: List<ProductoApi> = emptyList()
    private lateinit var lista: LinearLayout
    private lateinit var vacio: TextView
    private lateinit var total: TextView
    private lateinit var nota: EditText
    private lateinit var enviar: TextView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun texto(t: String, size: Float, colorRes: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = t
            textSize = size
            setTextColor(ContextCompat.getColor(this@CatalogoActivity, colorRes))
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

    private fun boton(t: String, accion: () -> Unit) =
        texto(t, 14f, R.color.ink, true).apply {
            setBackgroundResource(R.drawable.bg_card)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            setOnClickListener { accion() }
        }

    private fun cant(v: Double) =
        if (v % 1.0 == 0.0) String.format(Locale.US, "%.0f", v) else String.format(Locale.US, "%.2f", v)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (proveedorId < 0) { finish(); return }

        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            fitsSystemWindows = true
            setBackgroundColor(ContextCompat.getColor(this@CatalogoActivity, R.color.app_bg))
        }

        val encabezado = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(8))
        }
        val titulo = texto(intent.getStringExtra("proveedor_nombre") ?: "Cat\u00e1logo", 22f, R.color.ink, true).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        encabezado.addView(titulo)
        encabezado.addView(boton("Volver") { finish() })

        vacio = texto("Cargando\u2026", 14f, R.color.muted).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, 0)
        }
        lista = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val contenido = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), 0, dp(20), dp(16))
            addView(vacio)
            addView(lista)
        }
        val scroll = ScrollView(this).apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            addView(contenido)
        }

        nota = EditText(this).apply {
            hint = "Nota para el proveedor (opcional)"
            setTextColor(ContextCompat.getColor(this@CatalogoActivity, R.color.ink))
            setHintTextColor(ContextCompat.getColor(this@CatalogoActivity, R.color.muted))
        }
        total = texto("Total: \$0.00", 15f, R.color.ink, true).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        enviar = boton("Enviar pedido") { enviarPedido() }
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, 0)
            addView(total)
            addView(enviar)
        }
        val barra = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card)
            setPadding(dp(20), dp(12), dp(20), dp(12))
            addView(nota)
            addView(fila)
        }

        raiz.addView(encabezado)
        raiz.addView(scroll)
        raiz.addView(barra)
        setContentView(raiz)

        cargar()
    }

    private fun cargar() {
        lifecycleScope.launch {
            repo.catalogoProveedor(proveedorId)
                .onSuccess { productos = it; pintar() }
                .onFailure { vacio.text = it.message ?: "Error al cargar" }
        }
    }

    private fun actualizarTotal() {
        var t = 0.0
        productos.forEach { p -> t += p.precio * (cantidades[p.id] ?: 0) }
        total.text = String.format(Locale.US, "Total: $%.2f", t)
    }

    private fun pintar() {
        lista.removeAllViews()
        vacio.text = "Este proveedor no tiene productos disponibles"
        vacio.visibility = if (productos.isEmpty()) View.VISIBLE else View.GONE
        productos.forEach { p ->
            val max = p.stock.toInt()
            val qty = texto((cantidades[p.id] ?: 0).toString(), 16f, R.color.ink, true).apply {
                gravity = Gravity.CENTER
                minWidth = dp(32)
            }
            fun cambiar(d: Int) {
                val n = ((cantidades[p.id] ?: 0) + d).coerceIn(0, max)
                cantidades[p.id] = n
                qty.text = n.toString()
                actualizarTotal()
            }
            val izq = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                addView(texto(p.nombre, 16f, R.color.ink, true))
                if (p.descripcion.isNotBlank()) addView(texto(p.descripcion, 13f, R.color.muted))
                addView(texto(String.format(Locale.US, "$%.2f \u00b7 Quedan %s", p.precio, cant(p.stock)), 13f, R.color.ink))
            }
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundResource(R.drawable.bg_card)
                setPadding(dp(16), dp(12), dp(16), dp(12))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(12) }
                addView(izq)
                addView(boton("\u2212") { cambiar(-1) })
                addView(qty)
                addView(boton("+") { cambiar(1) })
            }
            lista.addView(card)
        }
        actualizarTotal()
    }

    private fun enviarPedido() {
        val items = cantidades.filter { it.value > 0 }.map { it.key to it.value.toDouble() }
        if (items.isEmpty()) {
            Toast.makeText(this, "Elige al menos un producto", Toast.LENGTH_SHORT).show()
            return
        }
        enviar.isEnabled = false
        enviar.alpha = 0.6f
        lifecycleScope.launch {
            repo.crearPedido(proveedorId, nota.text.toString().trim(), items)
                .onSuccess {
                    Toast.makeText(this@CatalogoActivity, "Pedido enviado", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@CatalogoActivity, PedidosActivity::class.java))
                    finish()
                }
                .onFailure {
                    enviar.isEnabled = true
                    enviar.alpha = 1f
                    Toast.makeText(this@CatalogoActivity, it.message ?: "Error", Toast.LENGTH_LONG).show()
                }
        }
    }
}
