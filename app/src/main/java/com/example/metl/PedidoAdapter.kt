package com.example.metl

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

data class PedidoItem(
    val id: Int,
    val estado: String,        // pendiente, aceptado, rechazado, entregado
    val nota: String?,
    val creadoEn: String?,
    val contraparte: String    // proveedor (si eres cliente) o cliente (si eres proveedor)
)

class PedidoAdapter(
    private val esProveedor: Boolean,
    private val onEstado: (PedidoItem, String) -> Unit
) : ListAdapter<PedidoItem, PedidoAdapter.VH>(object : DiffUtil.ItemCallback<PedidoItem>() {
    override fun areItemsTheSame(a: PedidoItem, b: PedidoItem) = a.id == b.id
    override fun areContentsTheSame(a: PedidoItem, b: PedidoItem) = a == b
}) {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val titulo: TextView = v.findViewById(R.id.tvPedidoTitulo)
        val estado: TextView = v.findViewById(R.id.tvEstado)
        val nota: TextView = v.findViewById(R.id.tvPedidoNota)
        val fecha: TextView = v.findViewById(R.id.tvPedidoFecha)
        val acciones: LinearLayout = v.findViewById(R.id.layoutAcciones)
        val aceptar: Button = v.findViewById(R.id.btnAceptar)
        val rechazar: Button = v.findViewById(R.id.btnRechazar)
        val entregado: Button = v.findViewById(R.id.btnEntregado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_pedido, parent, false))

    override fun onBindViewHolder(h: VH, position: Int) {
        val p = getItem(position)

        h.titulo.text = h.itemView.context.getString(R.string.pedido_titulo, p.id, p.contraparte)
        h.estado.text = p.estado.replaceFirstChar { it.uppercase() }
        h.estado.setTextColor(
            when (p.estado) {
                "pendiente" -> Color.parseColor("#B26A00")
                "aceptado" -> Color.parseColor("#1565C0")
                "entregado" -> Color.parseColor("#2E7D32")
                "rechazado" -> Color.parseColor("#C62828")
                else -> Color.DKGRAY
            }
        )
        h.nota.text = p.nota.orEmpty()
        h.nota.visibility = if (p.nota.isNullOrBlank()) View.GONE else View.VISIBLE
        h.fecha.text = p.creadoEn?.take(10).orEmpty()

        // Solo el proveedor ve botones, y según el estado actual
        val verAcciones = esProveedor && (p.estado == "pendiente" || p.estado == "aceptado")
        h.acciones.visibility = if (verAcciones) View.VISIBLE else View.GONE
        h.aceptar.visibility = if (p.estado == "pendiente") View.VISIBLE else View.GONE
        h.rechazar.visibility = if (p.estado == "pendiente") View.VISIBLE else View.GONE
        h.entregado.visibility = if (p.estado == "aceptado") View.VISIBLE else View.GONE

        h.aceptar.setOnClickListener { onEstado(p, "aceptado") }
        h.rechazar.setOnClickListener { onEstado(p, "rechazado") }
        h.entregado.setOnClickListener { onEstado(p, "entregado") }
    }
}