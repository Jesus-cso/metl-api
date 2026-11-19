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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.tvYaTengo.setOnClickListener { finish() }

        b.btnRegistrar.setOnClickListener {
            val nombre = b.etNombre.text.toString().trim()
            val correo = b.etCorreoR.text.toString().trim()
            val pass = b.etPassR.text.toString()
            val pass2 = b.etPass2.text.toString()
            val rol = if (b.rbProductor.isChecked) Rol.PRODUCTOR else Rol.CLIENTE

            val error = when {
                nombre.isBlank() -> "Escribe tu nombre"
                !Patterns.EMAIL_ADDRESS.matcher(correo).matches() -> "Escribe un correo válido"
                pass.length < 8 -> "La contraseña debe tener al menos 8 caracteres"
                pass != pass2 -> "Las contraseñas no coinciden"
                else -> null
            }
            if (error != null) {
                vm.registroError.value = error
            } else {
                vm.registrar(nombre, correo, pass, rol)
            }
        }

        vm.registroLoading.observe(this) { cargando ->
            b.btnRegistrar.isEnabled = !cargando
            b.btnRegistrar.alpha = if (cargando) 0.6f else 1f
            b.btnRegistrar.text = if (cargando) "Creando…" else "Crear cuenta"
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