const express = require('express');
const db = require('../db');
const { auth, soloRol, proveedorActual } = require('../middleware/auth');

const router = express.Router();
router.use(auth, soloRol('proveedor'), proveedorActual);

// Convierte una fila de magueyes (+ su recomendación) al JSON que usa la app
function aMaguey(m, rec) {
  return {
    id: m.codigo,
    variedad: m.variedad,
    edadAnios: m.edad_anios,
    ubicacion: m.ubicacion,
    humedad: m.humedad,
    temperatura: m.temperatura,
    estado: m.estado, // capar_pronto | riego_bajo | en_desarrollo
    nota: m.nota,
    historialHumedad: [], 
    recomendacion: rec
      ? { titulo: rec.titulo, confianza: rec.confianza, detalle: rec.detalle }
      : null,
  };
}

// Trae la recomendación más reciente de cada maguey
async function recomendacionesPor(ids) {
  if (ids.length === 0) return new Map();
  const marcas = ids.map(() => '?').join(',');
  const [rows] = await db.query(
    `SELECT maguey_id, titulo, confianza, detalle
       FROM recomendaciones
      WHERE maguey_id IN (${marcas})
      ORDER BY id ASC`,
    ids
  );
  const mapa = new Map();
  rows.forEach((r) => mapa.set(r.maguey_id, r));
  return mapa;
}

// GET /proveedor/resumen
router.get('/resumen', async (req, res) => {
  const [parcelas] = await db.query(
    'SELECT nombre, municipio FROM parcelas WHERE proveedor_id = ? ORDER BY id LIMIT 1',
    [req.proveedorId]
  );
  const parcela = parcelas[0];

  const [conteo] = await db.query(
    `SELECT COUNT(*) AS total,
            SUM(m.estado = 'capar_pronto') AS listos,
            SUM(m.estado = 'riego_bajo') AS alertas
       FROM magueyes m
       JOIN parcelas p ON p.id = m.parcela_id
      WHERE p.proveedor_id = ?`,
    [req.proveedorId]
  );
  const [ped] = await db.query(
    `SELECT COUNT(*) AS n FROM pedidos
      WHERE proveedor_id = ? AND estado IN ('pendiente', 'aceptado')`,
    [req.proveedorId]
  );
  const [usr] = await db.query('SELECT nombre FROM usuarios WHERE id = ?', [req.user.id]);

  const abiertos = Number(ped[0].n);
  res.json({
    parcela: parcela ? `${parcela.nombre} · ${parcela.municipio}` : 'Sin parcela registrada',
    usuario: usr[0]?.nombre ?? '',
    totalSensores: Number(conteo[0].total),
    listosCapar: Number(conteo[0].listos || 0),
    alertas: Number(conteo[0].alertas || 0),
    pedidosAbiertos:
      abiertos === 0 ? 'No tienes pedidos abiertos'
      : abiertos === 1 ? '1 pedido abierto'
      : `${abiertos} pedidos abiertos`,
  });
});

// GET /proveedor/magueyes
router.get('/magueyes', async (req, res) => {
  const [rows] = await db.query(
    `SELECT m.id, m.codigo, m.variedad, m.edad_anios, m.ubicacion, m.humedad,
            m.temperatura, m.estado, m.nota
       FROM magueyes m
       JOIN parcelas p ON p.id = m.parcela_id
      WHERE p.proveedor_id = ?
      ORDER BY m.id`,
    [req.proveedorId]
  );
  const recs = await recomendacionesPor(rows.map((m) => m.id));
  res.json(rows.map((m) => aMaguey(m, recs.get(m.id))));
});

// GET /proveedor/magueyes/:codigo
router.get('/magueyes/:codigo', async (req, res) => {
  const [rows] = await db.query(
    `SELECT m.id, m.codigo, m.variedad, m.edad_anios, m.ubicacion, m.humedad,
            m.temperatura, m.estado, m.nota
       FROM magueyes m
       JOIN parcelas p ON p.id = m.parcela_id
      WHERE p.proveedor_id = ? AND m.codigo = ?`,
    [req.proveedorId, req.params.codigo]
  );
  if (rows.length === 0) return res.status(404).json({ error: 'Maguey no encontrado' });
  const recs = await recomendacionesPor([rows[0].id]);
  res.json(aMaguey(rows[0], recs.get(rows[0].id)));
});

// GET /proveedor/lotes/actual
router.get('/lotes/actual', async (req, res) => {
  const [lotes] = await db.query(
    'SELECT id, codigo FROM lotes WHERE proveedor_id = ? ORDER BY id DESC LIMIT 1',
    [req.proveedorId]
  );
  if (lotes.length === 0) return res.status(404).json({ error: 'Aún no tienes lotes' });

  const [pasos] = await db.query(
    'SELECT titulo, detalle FROM lote_pasos WHERE lote_id = ? ORDER BY orden',
    [lotes[0].id]
  );
  const [pedidos] = await db.query(
    `SELECT u.nombre AS cliente, p.nota AS detalle, p.estado
       FROM pedidos p
       JOIN usuarios u ON u.id = p.cliente_id
      WHERE p.proveedor_id = ?
      ORDER BY p.id DESC
      LIMIT 10`,
    [req.proveedorId]
  );
  res.json({ codigo: lotes[0].codigo, pasos, pedidos });
});

module.exports = router;