package com.example.metl

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ApiRepository(private val ctx: Context) : MetlRepository {

    companion object {
        // Emulador: 10.0.2.2 es tu PC. Celular real: la IP de tu PC (ipconfig)
        const val BASE_URL = "http://192.168.0.104:3100"
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
                            .put("nombre_negocio", NegocioPendiente.nombre.ifBlank { nombre.trim() })
                            .put("municipio", NegocioPendiente.municipio.ifBlank { "Tulancingo" })
                            .put("latitud", NegocioPendiente.latitud)
                            .put("longitud", NegocioPendiente.longitud)
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
                val est = p.texto("estado")
                if (est == "rechazado" || est == "cancelado") continue   // un pedido rechazado ya no se ve como confirmado
                val estado = if (est == "pendiente")
                    EstadoPedido.POR_ACEPTAR else EstadoPedido.CONFIRMADO
                pedidos.add(Pedido(p.texto("cliente"), p.texto("detalle"), estado))
            }
        }
        Lote(codigo = o.texto("codigo").ifBlank { "Sin lotes" }, pasos = pasos, pedidos = pedidos)
    }

    override suspend fun refrescar() {
        productos(); magueyes()
    }

    // ---------- Pedidos ----------
    private fun parsePedidos(texto: String, campoContraparte: String): List<PedidoApi> {
        val a = JSONArray(texto)
        val salida = mutableListOf<PedidoApi>()
        for (i in 0 until a.length()) {
            val p = a.getJSONObject(i)
            val items = mutableListOf<PedidoItemApi>()
            p.optJSONArray("items")?.let { ia ->
                for (j in 0 until ia.length()) {
                    val x = ia.getJSONObject(j)
                    items.add(PedidoItemApi(x.texto("nombre"), x.optDouble("cantidad"), x.optDouble("precio_unit")))
                }
            }
            salida.add(
                PedidoApi(
                    id = p.getInt("id"),
                    estado = p.texto("estado"),
                    nota = if (p.isNull("nota")) "" else p.texto("nota"),
                    contraparte = p.texto(campoContraparte),
                    items = items
                )
            )
        }
        return salida
    }

    suspend fun crearPedido(proveedorId: Int, nota: String, items: List<Pair<Int, Double>>): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val arr = JSONArray()
                items.forEach { (productoId, cantidad) ->
                    arr.put(JSONObject().put("producto_id", productoId).put("cantidad", cantidad))
                }
                val body = JSONObject()
                    .put("proveedor_id", proveedorId)
                    .put("nota", nota)
                    .put("items", arr)
                Result.success(JSONObject(request("POST", "/pedidos", body)).getInt("id"))
            } catch (e: Exception) {
                Result.failure(Exception(e.message ?: "No se pudo crear el pedido"))
            }
        }

    suspend fun misPedidos(): Result<List<PedidoApi>> = withContext(Dispatchers.IO) {
        try {
            Result.success(parsePedidos(request("GET", "/pedidos/mios"), "proveedor"))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "No se pudieron cargar tus pedidos"))
        }
    }

    suspend fun pedidosRecibidos(): Result<List<PedidoApi>> = withContext(Dispatchers.IO) {
        try {
            Result.success(parsePedidos(request("GET", "/pedidos/recibidos"), "cliente"))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "No se pudieron cargar los pedidos"))
        }
    }

    suspend fun cambiarEstado(id: Int, estado: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            request("POST", "/pedidos/$id/estado", JSONObject().put("estado", estado))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "No se pudo actualizar el pedido"))
        }
    }

    // id de producto -> id de proveedor (para poder hacer pedidos)
    suspend fun proveedoresPorProducto(): Result<Map<Int, Int>> = withContext(Dispatchers.IO) {
        try {
            val a = JSONArray(request("GET", "/productos/publicos"))
            val m = mutableMapOf<Int, Int>()
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                m[o.getInt("id")] = o.getInt("proveedor_id")
            }
            Result.success(m)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "No se pudieron cargar los productos"))
        }
    }

    // ---------- Productos del proveedor ----------
    suspend fun misProductos(): Result<List<ProductoApi>> = withContext(Dispatchers.IO) {
        try {
            val a = JSONArray(request("GET", "/productos/proveedor"))
            val salida = mutableListOf<ProductoApi>()
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                val act = o.opt("activo")
                val activo = act == true || (act is Number && act.toInt() == 1) || act == "1"
                salida.add(
                    ProductoApi(
                        id = o.getInt("id"),
                        nombre = o.texto("nombre"),
                        descripcion = o.texto("descripcion"),
                        precio = o.optDouble("precio"),
                        stock = o.optDouble("stock"),
                        activo = activo
                    )
                )
            }
            Result.success(salida)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "No se pudieron cargar tus productos"))
        }
    }

    // id = null crea un producto nuevo; con id, lo actualiza
    suspend fun guardarProducto(
        id: Int?, nombre: String, descripcion: String, precio: Double, stock: Double, activo: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject()
                .put("nombre", nombre)
                .put("descripcion", descripcion)
                .put("precio", precio)
                .put("stock", stock)
            if (id == null) {
                request("POST", "/productos", body)
            } else {
                body.put("activo", activo)
                request("PUT", "/productos/$id", body)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "No se pudo guardar el producto"))
        }
    }
    // ---------- Proveedores y catalogo (cliente) ----------
    private fun JSONObject.txt(k: String): String = if (isNull(k)) "" else optString(k, "")

    suspend fun proveedoresCercanos(municipio: String): Result<List<ProveedorApi>> = withContext(Dispatchers.IO) {
        try {
            val ruta = if (municipio.isBlank()) "/proveedores/cercanos"
            else "/proveedores/cercanos?municipio=" + java.net.URLEncoder.encode(municipio.trim(), "UTF-8")
            val a = JSONArray(request("GET", ruta))
            val salida = mutableListOf<ProveedorApi>()
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                salida.add(ProveedorApi(o.getInt("id"), o.txt("nombre_negocio"), o.txt("descripcion"), o.txt("municipio")))
            }
            Result.success(salida)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "No se pudieron cargar los proveedores"))
        }
    }

    suspend fun catalogoProveedor(id: Int): Result<List<ProductoApi>> = withContext(Dispatchers.IO) {
        try {
            val arr = JSONObject(request("GET", "/proveedores/$id/catalogo")).getJSONArray("productos")
            val salida = mutableListOf<ProductoApi>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                salida.add(
                    ProductoApi(o.getInt("id"), o.txt("nombre"), o.txt("descripcion"), o.optDouble("precio"), o.optDouble("stock"), true)
                )
            }
            Result.success(salida)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "No se pudo cargar el catalogo"))
        }
    }
    suspend fun cancelarPedido(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            request("POST", "/pedidos/$id/cancelar", JSONObject())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "No se pudo cancelar el pedido"))
        }
    }}
