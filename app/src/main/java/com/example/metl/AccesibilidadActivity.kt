package com.example.metl

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat

class AccesibilidadActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Accesibilidad"
        val dp = resources.displayMetrics.density
        fun px(v: Int) = (v * dp).toInt()

        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(20), px(20), px(20), px(20))
        }

        raiz.addView(TextView(this).apply {
            text = "Tamaño de letra"
            textSize = 20f
            contentDescription = "Tamaño de letra"
        })

        val opciones = listOf(
            "Tamaño del sistema" to 0f,
            "Normal" to 1.0f,
            "Grande" to 1.3f,
            "Muy grande" to 1.6f
        )
        val valores = HashMap<Int, Float>()
        val actual = Accesibilidad.escala(this)
        val grupo = RadioGroup(this)
        opciones.forEach { (nombre, valor) ->
            val rb = RadioButton(this).apply {
                id = View.generateViewId()
                text = nombre
                textSize = 18f
                minHeight = px(48)
                isChecked = (valor == actual)
            }
            valores[rb.id] = valor
            grupo.addView(rb)
        }
        grupo.setOnCheckedChangeListener { _, id ->
            val v = valores[id] ?: return@setOnCheckedChangeListener
            Accesibilidad.guardarEscala(this, v)
            recreate()
        }
        raiz.addView(grupo)

        raiz.addView(SwitchCompat(this).apply {
            text = "Leer la pantalla en voz alta"
            textSize = 18f
            minHeight = px(48)
            isChecked = Accesibilidad.vozActiva(this@AccesibilidadActivity)
            setOnCheckedChangeListener { _, activo ->
                Accesibilidad.guardarVoz(this@AccesibilidadActivity, activo)
                if (activo) Voz.hablar(this@AccesibilidadActivity, "Voz activada. Leeré cada pantalla al abrirla.")
                else Voz.detener()
            }
        })

        raiz.addView(Button(this).apply {
            text = "Probar voz"
            minHeight = px(48)
            minimumHeight = px(48)
            setOnClickListener {
                Voz.hablar(this@AccesibilidadActivity, "Hola, soy la voz de Metl. Así sonaré al leer tus pantallas.")
            }
        })

        raiz.addView(SwitchCompat(this).apply {
            text = "Vibrar con los avisos"
            textSize = 18f
            minHeight = px(48)
            isChecked = Accesibilidad.vibrarActivo(this@AccesibilidadActivity)
            setOnCheckedChangeListener { _, activo ->
                Accesibilidad.guardarVibrar(this@AccesibilidadActivity, activo)
            }
        })

        raiz.addView(TextView(this).apply {
            text = "Los avisos importantes se quedan en pantalla hasta que los cierres."
            textSize = 16f
            setPadding(0, px(12), 0, px(12))
        })

        raiz.addView(Button(this).apply {
            text = "Volver"
            minHeight = px(48)
            minimumHeight = px(48)
            setOnClickListener { finish() }
        })

        setContentView(ScrollView(this).apply { addView(raiz) })
    }
}