package com.example.metl

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.metl.databinding.ActivityAjustesBinding
import java.util.Locale

class AjustesActivity : BaseActivity() {

    private lateinit var b: ActivityAjustesBinding

    private val pedirPermisoUbicacion =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
            if (r.values.any { it }) {
                obtenerUbicacion()
            } else {
                b.tvEstado.text = "Sin permiso de ubicación. Puedes escribir las coordenadas a mano."
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAjustesBinding.inflate(layoutInflater)
        setContentView(b.root)
        findViewById<android.view.View>(R.id.btnAccesibilidad).setOnClickListener {
            startActivity(android.content.Intent(this@AjustesActivity, AccesibilidadActivity::class.java))
        }

        val version = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrNull() ?: "1.0"
        b.tvVersion.text = "Versión $version"

        b.btnVolver.setOnClickListener { finish() }

        iniciarCuenta()
        iniciarUbicacion()
        iniciarTema()

        b.btnSalir.setOnClickListener { confirmarSalir() }
    }

    // ------------------------------------------------------------------ CUENTA

    private fun iniciarCuenta() {
        mostrarNombre(Session.nombre(this))
        b.etNombre.setText(Session.nombre(this))

        b.btnGuardarNombre.setOnClickListener {
            val nuevo = b.etNombre.text.toString().trim()
            if (nuevo.length < 2) {
                b.etNombre.error = "Mínimo 2 caracteres"
                return@setOnClickListener
            }
            Session.cambiarNombre(this, nuevo)
            mostrarNombre(nuevo)
            b.tvEstado.text = "Nombre actualizado."
        }
    }

    private fun mostrarNombre(guardado: String) {
        val nombre = guardado.ifBlank { "Mi cuenta" }
        b.tvNombre.text = nombre
        b.tvAvatar.text = nombre.firstOrNull()?.uppercase() ?: "?"
    }

    // --------------------------------------------------------------- UBICACIÓN

    private fun iniciarUbicacion() {
        Ajustes.latitud(this)?.let { b.etLatitud.setText(formato(it)) }
        Ajustes.longitud(this)?.let { b.etLongitud.setText(formato(it)) }

        b.btnUsarUbicacion.setOnClickListener {
            pedirPermisoUbicacion.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        b.btnGuardarUbicacion.setOnClickListener {
            val latTxt = b.etLatitud.text.toString().trim().replace(',', '.')
            val lonTxt = b.etLongitud.text.toString().trim().replace(',', '.')

            // Ambos vacíos = borrar ubicación
            if (latTxt.isEmpty() && lonTxt.isEmpty()) {
                Ajustes.guardarUbicacion(this, null, null)
                b.tvEstado.text = "Ubicación borrada."
                return@setOnClickListener
            }

            val lat = latTxt.toDoubleOrNull()
            val lon = lonTxt.toDoubleOrNull()

            if (lat == null || lat !in -90.0..90.0) {
                b.etLatitud.error = "Entre -90 y 90"
                return@setOnClickListener
            }
            if (lon == null || lon !in -180.0..180.0) {
                b.etLongitud.error = "Entre -180 y 180"
                return@setOnClickListener
            }

            Ajustes.guardarUbicacion(this, lat, lon)
            b.tvEstado.text = "Ubicación guardada: ${formato(lat)}, ${formato(lon)}"
        }
    }

    @SuppressLint("MissingPermission")
    private fun obtenerUbicacion() {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager

        val loc = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
            .maxByOrNull { it.time }

        if (loc == null) {
            Toast.makeText(
                this,
                "No hay ubicación disponible. Activa el GPS, abre Maps un momento y vuelve a intentar.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        b.etLatitud.setText(formato(loc.latitude))
        b.etLongitud.setText(formato(loc.longitude))
        b.tvEstado.text = "Ubicación obtenida. Toca \"Guardar\" para conservarla."
    }

    private fun formato(d: Double) = String.format(Locale.US, "%.6f", d)

    // ------------------------------------------------------------------- TEMA

    private fun iniciarTema() {
        b.rgTema.check(
            when (Ajustes.tema(this)) {
                1 -> R.id.rbClaro
                2 -> R.id.rbOscuro
                else -> R.id.rbSistema
            }
        )

        // El listener va después del check() inicial para no disparar un cambio al abrir
        b.rgTema.setOnCheckedChangeListener { _, id ->
            val tema = when (id) {
                R.id.rbClaro -> 1
                R.id.rbOscuro -> 2
                else -> 0
            }
            if (tema != Ajustes.tema(this)) Ajustes.guardarTema(this, tema)
        }
    }

    // ------------------------------------------------------------------ SESIÓN

    private fun confirmarSalir() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Seguro que quieres salir?")
            .setPositiveButton("Salir") { _, _ ->
                Session.cerrar(this)
                startActivity(
                    Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}