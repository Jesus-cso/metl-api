package com.example.metl

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

enum class Pantalla { PARCELA, DETALLE, MERCADO }

class MainActivity : AppCompatActivity() {

    private val vm: MetlViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.navParcela).setOnClickListener { irA(Pantalla.PARCELA) }
        findViewById<TextView>(R.id.navMagueyes).setOnClickListener { irA(Pantalla.DETALLE) }
        findViewById<TextView>(R.id.navMercado).setOnClickListener { irA(Pantalla.MERCADO) }

        agregarBotonesProveedor()

        if (savedInstanceState == null) irA(Pantalla.PARCELA)
    }

    // Agrega "Pedidos" y "Salir" a la barra, copiando el estilo de la pestaña Mercado
    private fun agregarBotonesProveedor() {
        val modelo = findViewById<TextView>(R.id.navMercado)
        val padre = modelo.parent as? LinearLayout ?: return

        fun clonar(etiqueta: String, accion: () -> Unit): TextView = TextView(this).apply {
            text = etiqueta
            layoutParams = LinearLayout.LayoutParams(modelo.layoutParams as LinearLayout.LayoutParams)
            background = modelo.background?.constantState?.newDrawable()?.mutate()
            setPadding(modelo.paddingLeft, modelo.paddingTop, modelo.paddingRight, modelo.paddingBottom)
            gravity = modelo.gravity
            setTextSize(TypedValue.COMPLEX_UNIT_PX, modelo.textSize)
            setTextColor(modelo.textColors)
            typeface = modelo.typeface
            isClickable = true
            isFocusable = true
            setOnClickListener { accion() }
        }

        padre.addView(clonar("Productos") {
            startActivity(Intent(this, ProductosActivity::class.java))
        })
        padre.addView(clonar("Pedidos") {
            startActivity(Intent(this, PedidosActivity::class.java))
        })
        padre.addView(clonar("Salir") {
            Session.cerrar(this)
            startActivity(
                Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
        })
    }

    fun irA(pantalla: Pantalla) {
        val fragmento: Fragment = when (pantalla) {
            Pantalla.PARCELA -> ParcelaFragment()
            Pantalla.DETALLE -> DetalleFragment()
            Pantalla.MERCADO -> MercadoFragment()
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragmento)
            .commit()
    }

    fun abrirMaguey(id: String) {
        vm.seleccionar(id)      // nombre supuesto, ver abajo
        irA(Pantalla.DETALLE)
    }
}
