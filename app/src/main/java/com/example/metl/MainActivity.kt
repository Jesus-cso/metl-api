package com.example.metl

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.metl.databinding.ActivityMainBinding

enum class Pantalla { PARCELA, DETALLE, MERCADO }

class MainActivity : BaseActivity() {

    private lateinit var b: ActivityMainBinding
    private val vm: MetlViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Los sensores son del proveedor: el cliente no ve esta opción
        b.nav.menu.findItem(R.id.nav_sensores).isVisible =
            Session.rol(this).equals("proveedor", ignoreCase = true)

        b.nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_magueyes -> { mostrar(Pantalla.PARCELA); true }
                R.id.nav_pedidos -> { abrir(PedidosActivity::class.java); false }
                R.id.nav_productos -> { abrir(ProductosActivity::class.java); false }
                R.id.nav_sensores -> { abrir(SensoresActivity::class.java); false }
                R.id.nav_ajustes -> { abrir(AjustesActivity::class.java); false }
                else -> false
            }
        }
        b.nav.setOnItemReselectedListener { }

        if (savedInstanceState == null) irA(Pantalla.PARCELA)
    }

    private fun abrir(c: Class<*>) = startActivity(Intent(this, c))

    /** Lo usan los fragments (ParcelaFragment). */
    fun irA(p: Pantalla) {
        if (p == Pantalla.MERCADO) {          // el mercado ya no existe: va a Pedidos
            abrir(PedidosActivity::class.java)
            return
        }
        mostrar(p)
        b.nav.menu.findItem(R.id.nav_magueyes).isChecked = true   // no dispara el listener
    }

    fun abrirMaguey(id: String) {
        vm.seleccionar(id)
        irA(Pantalla.DETALLE)
    }

    private fun mostrar(p: Pantalla) {
        val f: Fragment = when (p) {
            Pantalla.DETALLE -> DetalleFragment()
            else -> ParcelaFragment()
        }
        supportFragmentManager.beginTransaction().replace(R.id.container, f).commit()
    }
}