package com.example.metl

import android.os.Bundle
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

        if (savedInstanceState == null) irA(Pantalla.PARCELA)
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