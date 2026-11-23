
package com.example.metl

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.metl.databinding.ActivityClienteBinding
import com.example.metl.databinding.ItemProductoBinding
import kotlinx.coroutines.launch

class ClienteActivity : BaseActivity() {

    private lateinit var b: ActivityClienteBinding
    private val vm: MetlViewModel by viewModels()
    private val repo by lazy {
        ApiRepository(applicationContext)
    }

    private var proveedorDe: Map<Int, Int> = emptyMap()

    private fun dp(v: Int) =
        (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityClienteBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.tvSaludo.text = intent.getStringExtra("correo")?.let {
            "Hola, $it"
        } ?: Session.nombre(this).let {
            if (it.isBlank()) "" else "Hola, $it"
        }

        b.btnSalir.setOnClickListener {
            Session.cerrar(this)

            startActivity(
                Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )

            finish()
        }

        b.btnMisPedidos.setOnClickListener {
            startActivity(
                Intent(this, PedidosActivity::class.java)
            )
        }

        b.btnProveedores.setOnClickListener {
            startActivity(
                Intent(this, ProveedoresActivity::class.java)
            )
        }

        b.btnConfiguracion.setOnClickListener {
            startActivity(
                Intent(this, ConfiguracionActivity::class.java)
            )
        }

        vm.productos.observe(this) { lista ->

            b.listaProductos.removeAllViews()

            b.tvVacio.visibility =
                if (lista.isEmpty()) View.VISIBLE else View.GONE

            lista.forEach { p ->

                val item = ItemProductoBinding.inflate(
                    layoutInflater,
                    b.listaProductos,
                    false
                )

                item.tvNombre.text = p.nombre
                item.tvDescripcion.text = p.descripcion
                item.tvPresentacion.text =
                    "${p.presentacion} · Lote ${p.lote}"
                item.tvProductor.text = p.productor

                item.tvPrecio.text = String.format(
                    java.util.Locale.US,
                    "$%.2f",
                    p.precio
                )

                when {
                    p.disponibles == 0 ->
                        item.tvChip.aplicarChip(
                            "Agotado",
                            ChipTipo.BAD
                        )

                    p.disponibles <= 5 ->
                        item.tvChip.aplicarChip(
                            "Quedan ${p.disponibles}",
                            ChipTipo.WARN
                        )

                    else ->
                        item.tvChip.aplicarChip(
                            "Disponible",
                            ChipTipo.OK
                        )
                }

                item.root.setOnClickListener {
                    dialogoPedir(p)
                }

                b.listaProductos.addView(item.root)
            }
        }

        if (savedInstanceState == null) {
            vm.cargarProductos()
        }
    }

    override fun onResume() {
        super.onResume()
        cargarProveedores()
    }

    private fun cargarProveedores() {
        lifecycleScope.launch {
            repo.proveedoresPorProducto().onSuccess {
                proveedorDe = it
            }
        }
    }

    private fun dialogoPedir(p: Producto) {

        if (p.disponibles == 0) {
            Toast.makeText(
                this,
                "Producto agotado",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val productoId = p.id.toIntOrNull()
        val proveedorId = productoId?.let {
            proveedorDe[it]
        }

        if (productoId == null || proveedorId == null) {
            Toast.makeText(
                this,
                "Actualizando datos, intenta de nuevo en un momento",
                Toast.LENGTH_SHORT
            ).show()

            cargarProveedores()
            return
        }

        val cantidad = EditText(this).apply {
            hint = "Cantidad"
            inputType = InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText("1")
        }

        val nota = EditText(this).apply {
            hint = "Nota (opcional)"
        }

        val caja = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), 0)
            addView(cantidad)
            addView(nota)
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Pedir ${p.nombre}")
            .setView(caja)
            .setPositiveButton("Pedir") { _, _ ->

                val c = cantidad.text.toString().toDoubleOrNull()

                if (c == null || c <= 0) {
                    Toast.makeText(
                        this,
                        "Escribe una cantidad mayor a 0",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    repo.crearPedido(
                        proveedorId,
                        nota.text.toString().trim(),
                        listOf(productoId to c)
                    ).onSuccess {

                        Toast.makeText(
                            this@ClienteActivity,
                            "Pedido enviado",
                            Toast.LENGTH_SHORT
                        ).show()

                        startActivity(
                            Intent(
                                this@ClienteActivity,
                                PedidosActivity::class.java
                            )
                        )

                    }.onFailure {

                        Toast.makeText(
                            this@ClienteActivity,
                            it.message ?: "Error",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
