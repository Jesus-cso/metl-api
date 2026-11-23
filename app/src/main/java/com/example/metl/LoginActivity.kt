package com.example.metl
import com.example.metl.databinding.ActivityLoginBinding
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate


class LoginActivity : AppCompatActivity() {
    private val ActivityLoginBinding.tvCrear: Any
        get() {
            TODO()
        }

    private lateinit var b: ActivityLoginBinding
    private val vm: MetlViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        val noche = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        b.btnTema.text = if (noche) "☀" else "☾"

        b.btnTema.setOnClickListener {
            AppCompatDelegate.setDefaultNightMode(
                if (noche) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
            )
        }

        b.tvCrear.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }

        b.btnEntrar.setOnClickListener {
            vm.login(b.etCorreo.text.toString().trim(), b.etPass.text.toString())
        }

        vm.loginLoading.observe(this) { cargando ->
            b.btnEntrar.isEnabled = !cargando
            b.btnEntrar.alpha = if (cargando) 0.6f else 1f
            b.btnEntrar.text = if (cargando) "Entrando…" else "Entrar"
        }
        vm.loginError.observe(this) { msg ->
            b.tvError.text = msg ?: ""
            b.tvError.visibility = if (msg == null) View.GONE else View.VISIBLE
        }
        vm.loginOk.observe(this) { ok ->
            if (ok) {
                val destino = if (vm.loginRol.value == Rol.CLIENTE)
                    ClienteActivity::class.java else MainActivity::class.java
                startActivity(
                    Intent(this, destino).putExtra("correo", b.etCorreo.text.toString().trim())
                )
                finish()
            }
        }
    }
}