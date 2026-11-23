package com.example.metl

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Locale

class ProductosActivity : BaseActivity() {

    private val repo by lazy { ApiRepository(applicationContext) }
    private lateinit var lista: LinearLayout
    private lateinit var vacio: TextView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun texto(t: String, size: Float, colorRes: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = t
            textSize = size
            setTextColor(ContextCompat.getColor(this@ProductosActivity, colorRes))
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

    private fun boton(t: String, accion: () -> Unit) =
        texto(t, 14f, R.color.ink, true).apply {
            setBackgroundResource(R.drawable.bg_card)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            setOnClickListener { accion() }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            fitsSystemWindows = true
            setBackgroundColor(ContextCompat.getColor(this@ProductosActivity, R.color.app_bg))
        }

        val encabezado = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(8))
        }
        val titulo = texto("Mis productos", 26f, R.color.ink, true).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val nuevo = boton("Nuevo") {
            startActivity(Intent(this, PublicarActivity::class.java))
        }
        val volver = boton("Volver") { finish() }
        encabezado.addView(titulo)
        encabezado.addView(nuevo, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { marginEnd = dp(8) })
        encabezado.addView(volver)

        vacio = texto("Cargando\u2026", 14f, R.color.muted).apply {
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
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
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
            repo.misProductos()
                .onSuccess { pintar(it) }
                .onFailure {
                    lista.removeAllViews()
                    vacio.text = it.message ?: "Error al cargar"
                    vacio.visibility = View.VISIBLE
                }
        }
    }

    private fun cant(v: Double) =
        if (v % 1.0 == 0.0) String.format(Locale.US, "%.0f", v) else String.format(Locale.US, "%.2f", v)

    private fun pintar(productos: List<ProductoApi>) {
        lista.removeAllViews()
        vacio.text = "A\u00fan no tienes productos. Pulsa Nuevo."
        vacio.visibility = if (productos.isEmpty()) View.VISIBLE else View.GONE
        productos.forEach { p ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_card)
                setPadding(dp(16), dp(14), dp(16), dp(14))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(12) }
                isClickable = true
                setOnClickListener { dialogo(p) }
            }
            card.addView(texto(p.nombre, 16f, R.color.ink, true))
            if (p.descripcion.isNotBlank()) card.addView(texto(p.descripcion, 13f, R.color.muted))
            card.addView(
                texto(
                    String.format(Locale.US, "$%.2f \u00b7 Stock: %s", p.precio, cant(p.stock)),
                    14f, R.color.ink
                ).apply { setPadding(0, dp(6), 0, 0) }
            )
            card.addView(
                texto(
                    if (p.activo) "Visible para clientes" else "Oculto",
                    12f, R.color.muted
                )
            )
            lista.addView(card)
        }
    }

    // p == null: producto nuevo; si no, edita el existente
    private fun dialogo(p: ProductoApi?) {
        fun campo(hint: String, tipo: Int, valor: String) = EditText(this).apply {
            this.hint = hint
            inputType = tipo
            setText(valor)
        }
        val nombre = campo("Nombre", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, p?.nombre ?: "")
        val desc = campo("Descripci\u00f3n (opcional)", InputType.TYPE_CLASS_TEXT, p?.descripcion ?: "")
        val dec = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        val precio = campo("Precio", dec, p?.precio?.let { cant(it) } ?: "")
        val stock = campo("Stock", dec, p?.stock?.let { cant(it) } ?: "")
        val activo = CheckBox(this).apply {
            text = "Visible para clientes"
            isChecked = p?.activo ?: true
            visibility = if (p == null) View.GONE else View.VISIBLE
        }
        val caja = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), 0)
            addView(nombre); addView(desc); addView(precio); addView(stock); addView(activo)
        }

        AlertDialog.Builder(this)
            .setTitle(if (p == null) "Nuevo producto" else "Editar producto")
            .setView(caja)
            .setPositiveButton("Guardar") { _, _ ->
                val n = nombre.text.toString().trim()
                val pr = precio.text.toString().replace(',', '.').toDoubleOrNull()
                val st = stock.text.toString().replace(',', '.').toDoubleOrNull()
                if (n.isBlank() || pr == null || pr < 0 || st == null || st < 0) {
                    Toast.makeText(this, "Revisa nombre, precio y stock", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    repo.guardarProducto(p?.id, n, desc.text.toString().trim(), pr, st, activo.isChecked)
                        .onSuccess {
                            Toast.makeText(this@ProductosActivity, "Guardado", Toast.LENGTH_SHORT).show()
                            cargar()
                        }
                        .onFailure {
                            Toast.makeText(this@ProductosActivity, it.message ?: "Error", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}