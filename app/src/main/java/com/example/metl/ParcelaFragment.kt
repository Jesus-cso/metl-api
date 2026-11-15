package com.example.metl

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.metl.databinding.FragmentParcelaBinding
import com.example.metl.databinding.ItemFilaBinding

class ParcelaFragment : Fragment() {

    private var _b: FragmentParcelaBinding? = null
    private val b get() = _b!!
    private val vm: MetlViewModel by activityViewModels()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentParcelaBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val main = requireActivity() as MainActivity

        vm.resumen.observe(viewLifecycleOwner) { r ->
            if (r == null) return@observe
            b.tvUbicacion.text = r.parcela
            b.tvSaludo.text = "Hola, ${r.usuario}"
            b.tvSensores.text = r.totalSensores.toString()
            b.tvListos.text = r.listosCapar.toString()
            b.tvAlertas.text = r.alertas.toString()
            b.tvPedidos.text = r.pedidosAbiertos
        }

        vm.magueyes.observe(viewLifecycleOwner) { lista ->
            b.listaMagueyes.removeAllViews()
            lista.forEach { m ->
                val item = ItemFilaBinding.inflate(layoutInflater, b.listaMagueyes, false)
                item.tvTitulo.text = "Maguey ${m.id}"
                item.tvNota.text = m.nota
                item.tvChip.aplicarChip(m.estado.label, m.estado.chip)
                item.root.setOnClickListener { main.abrirMaguey(m.id) }
                b.listaMagueyes.addView(item.root)
            }
        }

        b.cardPedidos.setOnClickListener { main.irA(Pantalla.MERCADO) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}