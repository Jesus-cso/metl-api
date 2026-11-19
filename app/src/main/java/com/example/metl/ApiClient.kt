package com.example.metl

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ApiCliente(private val ctx: Context) : MetlRepository {

    companion object {
        // Emulador: 10.0.2.2 es tu PC. Celular real: http://192.168.0.109:3000
        const val BASE_URL = "http://192.168.100.3:3000"
    }

    private val productoDao = ProductoDao(ctx)
    private val magueyDao = MagueyDao(ctx)
    private val lecturaDao = LecturaDao(ctx)

    // ---------- HTTP ----------
    private fun request(metodo: String, ruta: String, cuerpo: JSONObject? = null): String {
        val con = URL(BASE_URL + ruta).openConnection() as HttpURLConnection
        try {
            con.requestMethod = metodo
            con.connectTimeout = 8000
            con.readTimeout = 8000
            con.setRequestProperty("Accept", "application/json")
            Session.token(ctx)?.let { con.setRequestProperty("Authorization", "Bearer $it") }
            if (cuerpo != null) {
                con.doOutput = true
                con.setRequestProperty("Content-Type", "application/json")
                con.outputStream.use { it.write(cuerpo.toString().toByteArray()) }
            }
            val status = con.responseCode
            val stream = if (status in 200..299) con.inputStream else con.errorStream
            val texto = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (status !in 200..299) {
                val msg = try {
                    JSONObject(texto).optString("error", "Error $status")
                } catch (_: Exception) {
                    "Error $status"
                }
                throw Exception(msg)
            }
            return texto
        } finally {
            con.disconnect()
        }
    }

    private fun rolDe(texto: String?): Rol =
        if (texto.orEmpty().equals("cliente", ignoreCase = true)) Rol.CLIENTE else Rol.PRODUCTOR

    // ---------- Auth ----------
    override suspend fun login(email: String, pass: String): Result<Rol> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().put("correo", email.trim()).put("password", pass)
            val o = JSONObject(request("POST", "/auth/login", body))
            val u = o.getJSONObject("usuario")
            Session.guardar(ctx, o.texto("token"), u.texto("rol"), u.texto("nombre"))
            Result.success(rolDe(u.texto("rol")))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "No se pudo conectar con el servidor"))
        }
    }

    override suspend fun registrar(nombre: String, email: String, pass: String, rol: Rol): Result<Rol> =
        withContext(Dispatchers.IO) {
            try {
                val rolApi = if (rol == Rol.CLIENTE) "cliente" else "proveedor"
                val body = JSONObject()
                    .put("nombre", nombre.trim())
                    .put("correo", email.trim())
                    .put("password", pass)
                    .put("rol", rolApi)
                if (rol != Rol.CLIENTE) {
                    // PROVISIONAL: el servidor exige datos del negocio y la pantalla aún no los pide.
                    body.put(
                        "negocio", JSONObject()
                            .put("nombre_negocio", nombre.trim())
                            .put("municipio", "Tulancingo")
                            .put("latitud", 20.0833)
                            .put("longitud", -98.3667)
                            .put("radio_km", 10)
                    )
                }
                val o = JSONObject(request("POST", "/auth/registro", body))
                val u = o.getJSONObject("usuario")
                Session.guardar(ctx, o.texto("token"), u.texto("rol"), u.texto("nombre"))
                Result.success(rol)
            } catch (e: Exception) {
                Result.failure(Exception(e.message ?: "No se pudo crear la cuenta"))
            }
        }

    // ---------- Datos (con respaldo local si no hay red) ----------
    override suspend fun productos(): List<Producto> = withContext(Dispatchers.IO) {
        val ruta = if (Session.rol(ctx) == "proveedor") "/productos/proveedor" else "/productos/publicos"
        try {
            productoDao.replaceAll(JSONArray(request("GET", ruta)))
        } catch (_: Exception) {
            // sin red: usa lo guardado
        }
        productoDao.listar()
    }

    override suspend fun magueyes(): List<Maguey> = withContext(Dispatchers.IO) {
        try {
            magueyDao.replaceAll(JSONArray(request("GET", "/proveedor/magueyes")))
        } catch (_: Exception) {
            // sin red: usa lo guardado
        }
        magueyDao.listar(lecturaDao)
    }

    override suspend fun resumen(): Resumen = withContext(Dispatchers.IO) {
        val texto = try {
            request("GET", "/proveedor/resumen").also { Cache.guardar(ctx, "resumen", it) }
        } catch (_: Exception) {
            Cache.leer(ctx, "resumen")
        }
        val o = try {
            JSONObject(texto ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
        Resumen(
            parcela = o.texto("parcela"),
            usuario = o.texto("usuario").ifBlank { Session.nombre(ctx) },
            totalSensores = o.optInt("totalSensores"),
            listosCapar = o.optInt("listosCapar"),
            alertas = o.optInt("alertas"),
            pedidosAbiertos = o.texto("pedidosAbiertos")
        )
    }

    override suspend fun lote(): Lote = withContext(Dispatchers.IO) {
        val texto = try {
            request("GET", "/proveedor/lotes/actual").also { Cache.guardar(ctx, "lote", it) }
        } catch (_: Exception) {
            Cache.leer(ctx, "lote")
        }
        val o = try {
            JSONObject(texto ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }

        val pasos = mutableListOf<Paso>()
        o.optJSONArray("pasos")?.let { a ->
            for (i in 0 until a.length()) {
                val p = a.getJSONObject(i)
                pasos.add(Paso(p.texto("titulo"), p.texto("detalle")))
            }
        }
        val pedidos = mutableListOf<Pedido>()
        o.optJSONArray("pedidos")?.let { a ->
            for (i in 0 until a.length()) {
                val p = a.getJSONObject(i)
                val estado = if (p.texto("estado") == "pendiente")
                    EstadoPedido.POR_ACEPTAR else EstadoPedido.CONFIRMADO
                pedidos.add(Pedido(p.texto("cliente"), p.texto("detalle"), estado))
            }
        }
        Lote(codigo = o.texto("codigo").ifBlank { "Sin lotes" }, pasos = pasos, pedidos = pedidos)
    }

    override suspend fun refrescar() {
        productos(); magueyes()
    }
}