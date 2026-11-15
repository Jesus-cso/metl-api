package com.example.metl
import kotlinx.coroutines.delay

interface MetlRepository {
    suspend fun login(email: String, pass: String): Result<Rol>
    suspend fun productos(): List<Producto>
    suspend fun registrar(nombre: String, email: String, pass: String, rol: Rol): Result<Rol>
    suspend fun resumen(): Resumen
    suspend fun magueyes(): List<Maguey>
    suspend fun lote(): Lote
}

object MetlGraph {
    val repo: MetlRepository = FakeMetlRepository()
}

class FakeMetlRepository : MetlRepository {

    override suspend fun login(email: String, pass: String): Result<Rol> {
        delay(500)
        if (pass.length < 4) {
            return Result.failure(Exception("Contraseña incorrecta (mínimo 4 caracteres)"))
        }
        // Prueba: un correo que empiece con "cliente" entra como cliente.
        val rol = if (email.startsWith("cliente", ignoreCase = true)) Rol.CLIENTE else Rol.PRODUCTOR
        return Result.success(rol)
    }

    override suspend fun registrar(nombre: String, email: String, pass: String, rol: Rol): Result<Rol> {
        delay(600)
        // Prueba: aquí iría la llamada a tu servidor para crear la cuenta.
        return Result.success(rol)
    }

    override suspend fun productos(): List<Producto> {
        delay(300)
        return listOf(
            Producto("P-001", "Pulque natural", "Fermentación tradicional, sabor suave.",
                "Litro", 45.0, 40, "PQ-118", "Metepantle Norte · Tulancingo"),
            Producto("P-002", "Pulque curado de guayaba", "Curado artesanal con fruta de temporada.",
                "Litro", 60.0, 12, "PQ-118", "Metepantle Norte · Tulancingo"),
            Producto("P-003", "Pulque curado de avena", "Cremoso, receta de la casa.",
                "Litro", 55.0, 4, "PQ-117", "Metepantle Norte · Tulancingo"),
            Producto("P-004", "Aguamiel fresca", "Recién recolectada, sin fermentar.",
                "Litro", 35.0, 25, "AG-052", "Metepantle Norte · Tulancingo"),
            Producto("P-005", "Pulque natural (barril)", "Para pulquerías y eventos.",
                "Barril 20 L", 780.0, 0, "PQ-116", "Metepantle Norte · Tulancingo")
        )
    }

    override suspend fun resumen() = Resumen(
        parcela = "Metepantle Norte · Tulancingo",
        usuario = "Don Mateo",
        totalSensores = 128,
        listosCapar = 14,
        alertas = 3,
        pedidosAbiertos = "2 pulquerías esperan lote de noviembre"
    )

    override suspend fun magueyes() = listOf(
        Maguey(
            id = "M-0457", variedad = "Pulquero", edadAnios = 11,
            ubicacion = "Fila 3, metepantle norte",
            humedad = 34, temperatura = 18, estado = EstadoMaguey.CAPAR_PRONTO,
            nota = "Humedad 34% · 11 años",
            historialHumedad = listOf(62, 64, 58, 66, 52, 47, 41, 37, 34),
            recomendacion = Recomendacion(
                "Ventana óptima de capado: 18 – 30 de octubre", 87,
                "basada en el desarrollo del cogollo y el historial de lecturas."
            )
        ),
        Maguey(
            id = "M-0312", variedad = "Pulquero", edadAnios = 9,
            ubicacion = "Fila 4, metepantle norte",
            humedad = 12, temperatura = 19, estado = EstadoMaguey.RIEGO_BAJO,
            nota = "Humedad 12% · sin lectura 2 h",
            historialHumedad = listOf(40, 36, 30, 26, 22, 19, 16, 14, 12),
            recomendacion = null
        ),
        Maguey(id = "M-0221", variedad = "Pulquero", edadAnios = 7,
            ubicacion = "Fila 5, metepantle norte",
            humedad = 55, temperatura = 17, estado = EstadoMaguey.EN_DESARROLLO,
            nota = "Crecimiento normal · 7 años",
            historialHumedad = listOf(58, 57, 56, 57, 55, 56, 54, 55, 55),
            recomendacion = null
        )
    )

    override suspend fun lote() = Lote(
        codigo = "PQ-118",
        pasos = listOf(
            Paso("Capado", "parcela Metepantle Norte"),
            Paso("Raspado y recolección", "registro del tlachiquero"),
            Paso("Fermentación", "control de temperatura"),
            Paso("Envasado", "lote listo para venta")
        ),
        pedidos = listOf(
            Pedido("Pulquería del centro", "Pedido recurrente · semanal", EstadoPedido.CONFIRMADO),
            Pedido("Restaurante de cocina tradicional", "Solicitud nueva", EstadoPedido.POR_ACEPTAR)
        )
    )
}