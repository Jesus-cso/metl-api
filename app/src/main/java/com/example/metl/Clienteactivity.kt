package com.example.metl

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.metl.databinding.ActivityClienteBinding
import com.example.metl.databinding.ItemProductoBinding

class ClienteActivity : AppCompatActivity() {

    private lateinit var b: ActivityClienteBinding
    private val vm: MetlViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityClienteBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.tvSaludo.text = intent.getStringExtra("correo")?.let { "Hola, $it" } ?: ""

        b.btnSalir.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        vm.productos.observe(this) { lista ->
            b.listaProductos.removeAllViews()
            b.tvVacio.visibility = if (lista.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            lista.forEach { p ->
                val item = ItemProductoBinding.inflate(layoutInflater, b.listaProductos, false)
                item.tvNombre.text = p.nombre
                item.tvDescripcion.text = p.descripcion
                item.tvPresentacion.text = "${p.presentacion} · Lote ${p.lote}"
                item.tvProductor.text = p.productor
                item.tvPrecio.text = String.format(java.util.Locale.US, "$%.2f", p.precio)
                when {
                    p.disponibles == 0 -> item.tvChip.aplicarChip("Agotado", ChipTipo.BAD)
                    p.disponibles <= 5 -> item.tvChip.aplicarChip("Quedan ${p.disponibles}", ChipTipo.WARN)
                    else -> item.tvChip.aplicarChip("Disponible", ChipTipo.OK)
                }
                b.listaProductos.addView(item.root)
            }
        }
        if (savedInstanceState == null) vm.cargarProductos()
    }
}