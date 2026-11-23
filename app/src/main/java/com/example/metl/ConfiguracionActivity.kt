package com.example.metl

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class ConfiguracionActivity : BaseActivity() {

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Configuración"

        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        raiz.addView(TextView(this).apply {
            text = "Configuración"
            textSize = 26f
            setPadding(0, 0, 0, dp(8))
        })

        raiz.addView(TextView(this).apply {
            text = "Personaliza tu experiencia en METL."
            textSize = 16f
            setPadding(0, 0, 0, dp(20))
        })

        raiz.addView(TextView(this).apply {
            text = "Accesibilidad"
            textSize = 20f
            setPadding(0, 0, 0, dp(8))
        })

        raiz.addView(Button(this).apply {
            text = "Configurar accesibilidad"
            minHeight = dp(48)

            setOnClickListener {
                startActivity(
                    Intent(
                        this@ConfiguracionActivity,
                        AccesibilidadActivity::class.java
                    )
                )
            }
        })

        raiz.addView(TextView(this).apply {
            text = "Cuenta"
            textSize = 20f
            setPadding(0, dp(24), 0, dp(8))
        })

        raiz.addView(Button(this).apply {
            text = "Cerrar sesión"
            minHeight = dp(48)

            setOnClickListener {
                Session.cerrar(this@ConfiguracionActivity)

                startActivity(
                    Intent(
                        this@ConfiguracionActivity,
                        LoginActivity::class.java
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )

                finish()
            }
        })

        raiz.addView(Button(this).apply {
            text = "Volver"
            minHeight = dp(48)

            setOnClickListener {
                finish()
            }
        })

        val scroll = android.widget.ScrollView(this).apply {
            addView(raiz)
        }

        setContentView(scroll)
    }
}
