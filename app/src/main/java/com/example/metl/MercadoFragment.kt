package com.example.metl

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.metl.databinding.FragmentMercadoBinding
import com.example.metl.databinding.ItemFilaBinding

class MercadoFragment : Fragment() {

    private var _b: FragmentMercadoBinding? = null
    private val b get() = _b!!
    private val vm: MetlViewModel by activityViewModels()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentMercadoBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vm.lote.observe(viewLifecycleOwner) { lote ->
            if (lote == null) {
                b.tvLote.text = "Cargando…"
                return@observe
            }
            b.tvLote.text = "Lote ${lote.codigo}"

            b.listaPasos.removeAllViews()
            val margen = (12 * resources.displayMetrics.density).toInt()
            lote.pasos.forEachIndexed { idx, p ->
                val texto = SpannableStringBuilder(p.titulo).apply {
                    setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    append(" · ${p.detalle}")
                }
                val tv = TextView(requireContext()).apply {
                    text = texto
                    setTextColor(requireContext().getColor(R.color.ink))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                }
                val lp = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
                if (idx > 0) lp.topMargin = margen
                b.listaPasos.addView(tv, lp)
            }

            b.listaPedidos.removeAllViews()
            lote.pedidos.forEach { p ->
                val item = ItemFilaBinding.inflate(layoutInflater, b.listaPedidos, false)
                item.tvTitulo.text = p.cliente
                item.tvNota.text = p.detalle
                item.tvChip.aplicarChip(p.estado.label, p.estado.chip)
                b.listaPedidos.addView(item.root)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}