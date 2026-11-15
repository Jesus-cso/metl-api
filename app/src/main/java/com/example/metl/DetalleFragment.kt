package com.example.metl

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.metl.databinding.FragmentDetalleBinding

class DetalleFragment : Fragment() {

    private var _b: FragmentDetalleBinding? = null
    private val b get() = _b!!
    private val vm: MetlViewModel by activityViewModels()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentDetalleBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val main = requireActivity() as MainActivity

        b.tvVolver.setOnClickListener { main.irA(Pantalla.PARCELA) }
        b.btnCapado.setOnClickListener { main.irA(Pantalla.MERCADO) }

        vm.seleccionado.observe(viewLifecycleOwner) { m ->
            if (m == null) {
                b.tvTitulo.text = "Cargando…"
                return@observe
            }
            b.tvTitulo.text = "Maguey ${m.id}"
            b.tvSub.text = "${m.variedad} · ${m.edadAnios} años · ${m.ubicacion}"
            b.tvHumedad.text = "${m.humedad}%"
            b.tvTemp.text = "${m.temperatura} °C"
            b.chart.setValores(m.historialHumedad.filterNotNull())

            val r = m.recomendacion
            if (r == null) {
                b.cardIa.visibility = View.GONE
            } else {
                b.cardIa.visibility = View.VISIBLE
                b.tvIaTitulo.text = r.titulo
                b.tvIaDetalle.text = "Confianza ${r.confianza}% · ${r.detalle}"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}