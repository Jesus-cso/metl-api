package com.example.metl

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.metl.databinding.ActivityRegistroBinding

class RegistroActivity : AppCompatActivity() {

    private lateinit var b: ActivityRegistroBinding
    private val vm: MetlViewModel by viewModels()

    private fun numero(t: String): Double? = t.trim().replace(',', '.').toDoubleOrNull()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.tvYaTengo.setOnClickListener { finish() }

        // Los datos del negocio solo se piden a los productores
        b.rgRol.setOnCheckedChangeListener { _, id ->
            b.llNegocio.visibility = if (id == b.rbProductor.id) View.VISIBLE else View.GONE
        }

        b.btnRegistrar.setOnClickListener {
            val nombre = b.etNombre.text.toString().trim()
            val correo = b.etCorreoR.text.toString().trim()
            val pass = b.etPassR.text.toString()
            val pass2 = b.etPass2.text.toString()
            val esProductor = b.rbProductor.isChecked
            val rol = if (esProductor) Rol.PRODUCTOR else Rol.CLIENTE

            val negocio = b.etNegocio.text.toString().trim()
            val municipio = b.etMunicipio.text.toString().trim()
            val lat = numero(b.etLatitud.text.toString())
            val lon = numero(b.etLongitud.text.toString())

            val error = when {
                nombre.isBlank() -> "Escribe tu nombre"
                !Patterns.EMAIL_ADDRESS.matcher(correo).matches() -> "Escribe un correo v\u00e1lido"
                pass.length < 8 -> "La contrase\u00f1a debe tener al menos 8 caracteres"
                pass != pass2 -> "Las contrase\u00f1as no coinciden"
                esProductor && negocio.isBlank() -> "Escribe el nombre de tu negocio"
                esProductor && municipio.isBlank() -> "Escribe tu municipio"
                esProductor && (lat == null || lat < -90 || lat > 90) -> "Latitud no v\u00e1lida (entre -90 y 90)"
                esProductor && (lon == null || lon < -180 || lon > 180) -> "Longitud no v\u00e1lida (entre -180 y 180)"
                else -> null
            }
            if (error != null) {
                vm.registroError.value = error
            } else {
                if (esProductor) {
                    NegocioPendiente.nombre = negocio
                    NegocioPendiente.municipio = municipio
                    NegocioPendiente.latitud = lat ?: 0.0
                    NegocioPendiente.longitud = lon ?: 0.0
                }
                vm.registrar(nombre, correo, pass, rol)
            }
        }

        vm.registroLoading.observe(this) { cargando ->
            b.btnRegistrar.isEnabled = !cargando
            b.btnRegistrar.alpha = if (cargando) 0.6f else 1f
            b.btnRegistrar.text = if (cargando) "Creando\u2026" else "Crear cuenta"
        }
        vm.registroError.observe(this) { msg ->
            b.tvErrorR.text = msg ?: ""
            b.tvErrorR.visibility = if (msg == null) View.GONE else View.VISIBLE
        }
        vm.registroRol.observe(this) { rol ->
            if (rol != null) {
                Toast.makeText(this, "Cuenta creada", Toast.LENGTH_SHORT).show()
                val destino = if (rol == Rol.CLIENTE)
                    ClienteActivity::class.java else MainActivity::class.java
                startActivity(
                    Intent(this, destino).putExtra("correo", b.etCorreoR.text.toString().trim())
                )
                finishAffinity()
            }
        }
    }
}
