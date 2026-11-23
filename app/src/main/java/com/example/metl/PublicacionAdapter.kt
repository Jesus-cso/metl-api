package com.example.metl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

data class Publicacion(
    val id: Int,
    val nombre: String,
    val descripcion: String?,
    val precio: Double,
    val stock: Int,
    val proveedorId: Int,
    val proveedor: String,
    val municipio: String?,
    val fotoUrl: String?
)

class PublicacionAdapter(
    private val onPedir: (Publicacion) -> Unit
) : ListAdapter<Publicacion, PublicacionAdapter.VH>(object : DiffUtil.ItemCallback<Publicacion>() {
    override fun areItemsTheSame(a: Publicacion, b: Publicacion) = a.id == b.id
    override fun areContentsTheSame(a: Publicacion, b: Publicacion) = a == b
}) {

    private val pesos = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX"))

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvProveedor: TextView = v.findViewById(R.id.tvProveedor)
        val tvMunicipio: TextView = v.findViewById(R.id.tvMunicipio)
        val ivFoto: ImageView = v.findViewById(R.id.ivFoto)
        val tvNombre: TextView = v.findViewById(R.id.tvNombre)
        val tvDescripcion: TextView = v.findViewById(R.id.tvDescripcion)
        val tvPrecio: TextView = v.findViewById(R.id.tvPrecio)
        val tvStock: TextView = v.findViewById(R.id.tvStock)
        val btnPedir: Button = v.findViewById(R.id.btnPedir)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_publicacion, parent, false))

    override fun onBindViewHolder(h: VH, position: Int) {
        val p = getItem(position)
        val ctx = h.itemView.context

        h.tvProveedor.text = p.proveedor
        h.tvMunicipio.text = p.municipio.orEmpty()
        h.tvNombre.text = p.nombre
        h.tvDescripcion.text = p.descripcion.orEmpty()
        h.tvDescripcion.visibility = if (p.descripcion.isNullOrBlank()) View.GONE else View.VISIBLE
        h.tvPrecio.text = pesos.format(p.precio)

        val hayStock = p.stock > 0
        h.tvStock.text =
            if (hayStock) ctx.getString(R.string.disponibles, p.stock) else ctx.getString(R.string.agotado)
        h.btnPedir.isEnabled = hayStock
        h.btnPedir.setOnClickListener { onPedir(p) }

        val url = p.fotoUrl?.let { foto ->
            if (foto.startsWith("http")) foto
            else "${Config.baseUrl}/${foto.trimStart('/')}"
        }
        Imagenes.cargar(h.ivFoto, url)
    }
}