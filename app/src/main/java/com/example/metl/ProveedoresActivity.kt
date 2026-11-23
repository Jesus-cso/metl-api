package com.example.metl

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ProveedoresActivity : BaseActivity() {

    private val repo by lazy { ApiRepository(applicationContext) }
    private lateinit var lista: LinearLayout
    private lateinit var vacio: TextView
    private lateinit var busqueda: EditText

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun texto(t: String, size: Float, colorRes: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = t
            textSize = size
            setTextColor(ContextCompat.getColor(this@ProveedoresActivity, colorRes))
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
            setBackgroundColor(ContextCompat.getColor(this@ProveedoresActivity, R.color.app_bg))
        }

        val encabezado = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(8))
        }
        val titulo = texto("Proveedores", 26f, R.color.ink, true).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        encabezado.addView(titulo)
        encabezado.addView(boton("Volver") { finish() })

        busqueda = EditText(this).apply {
            hint = "Municipio (opcional)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            isSingleLine = true
            setTextColor(ContextCompat.getColor(this@ProveedoresActivity, R.color.ink))
            setHintTextColor(ContextCompat.getColor(this@ProveedoresActivity, R.color.muted))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val filaBusqueda = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), 0, dp(20), dp(8))
            addView(busqueda)
            addView(
                boton("Buscar") { cargar() },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = dp(8) }
            )
        }

        vacio = texto("Buscando\u2026", 14f, R.color.muted).apply {
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
        raiz.addView(filaBusqueda)
        raiz.addView(scroll)
        setContentView(raiz)

        cargar()
    }

    private fun cargar() {
        vacio.text = "Buscando\u2026"
        vacio.visibility = View.VISIBLE
        lista.removeAllViews()
        lifecycleScope.launch {
            repo.proveedoresCercanos(busqueda.text.toString())
                .onSuccess { pintar(it) }
                .onFailure { vacio.text = it.message ?: "Error al cargar" }
        }
    }

    private fun pintar(proveedores: List<ProveedorApi>) {
        lista.removeAllViews()
        vacio.text = "No hay proveedores en ese municipio"
        vacio.visibility = if (proveedores.isEmpty()) View.VISIBLE else View.GONE
        proveedores.forEach { p ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_card)
                setPadding(dp(16), dp(14), dp(16), dp(14))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(12) }
                isClickable = true
                setOnClickListener {
                    startActivity(
                        Intent(this@ProveedoresActivity, CatalogoActivity::class.java)
                            .putExtra("proveedor_id", p.id)
                            .putExtra("proveedor_nombre", p.nombre)
                    )
                }
            }
            card.addView(texto(p.nombre, 16f, R.color.ink, true))
            card.addView(texto(p.municipio, 13f, R.color.muted))
            if (p.descripcion.isNotBlank()) {
                card.addView(texto(p.descripcion, 13f, R.color.ink).apply { setPadding(0, dp(4), 0, 0) })
            }
            lista.addView(card)
        }
    }
}
