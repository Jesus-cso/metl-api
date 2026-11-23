const express = require('express');
const crypto = require('crypto');
const db = require('../db');
const { auth, soloRol, proveedorActual } = require('../middleware/auth');

const router = express.Router();

const manejar = (fn) => (req, res) =>
  Promise.resolve(fn(req, res)).catch((err) => {
    console.error(`[${req.method} ${req.originalUrl}]`, err);
    res.status(500).json({ error: err.message || 'Error interno' });
  });

// ESP32 -> POST /sensores/lectura   (sin login, usa la clave del sensor)
router.post('/lectura', manejar(async (req, res) => {
  const key = req.get('x-api-key');
  if (!key) return res.status(401).json({ error: 'Falta x-api-key' });

  const [s] = await db.query('SELECT id FROM sensores WHERE api_key = ? AND activo = 1', [key]);
  if (s.length === 0) return res.status(401).json({ error: 'Clave de sensor no vÃ¡lida' });

  const { temperatura, humedad } = req.body || {};
  if (typeof temperatura !== 'number' || typeof humedad !== 'number') {
    return res.status(400).json({ error: 'temperatura y humedad deben ser nÃºmeros' });
  }
  if (temperatura < -40 || temperatura > 125 || humedad < 0 || humedad > 100) {
    return res.status(400).json({ error: 'Valores fuera de rango' });
  }

  await db.query(
    'INSERT INTO lecturas_sensor (sensor_id, temperatura, humedad) VALUES (?, ?, ?)',
    [s[0].id, temperatura, humedad]
  );
  res.status(201).json({ ok: true });
}));

// POST /sensores  -> registrar un sensor (devuelve la clave UNA vez)
router.post('/', auth, soloRol('proveedor'), proveedorActual, manejar(async (req, res) => {
  if (req.proveedorId == null) {
    return res.status(403).json({ error: 'Tu usuario no tiene un perfil de proveedor' });
  }
  const { nombre, latitud, longitud } = req.body || {};
  if (!nombre || !String(nombre).trim()) {
    return res.status(400).json({ error: 'Falta el nombre del sensor' });
  }

  const apiKey = crypto.randomBytes(24).toString('hex');
  const lat = latitud === undefined || latitud === null ? null : Number(latitud);
  const lon = longitud === undefined || longitud === null ? null : Number(longitud);

  const [r] = await db.query(
    'INSERT INTO sensores (proveedor_id, nombre, api_key, latitud, longitud) VALUES (?, ?, ?, ?, ?)',
    [req.proveedorId, String(nombre).trim(), apiKey, lat, lon]
  );
  res.status(201).json({ id: r.insertId, api_key: apiKey });
}));

// GET /sensores/proveedor  -> mis sensores con su Ãºltima lectura
router.get('/proveedor', auth, soloRol('proveedor'), proveedorActual, manejar(async (req, res) => {
  const [rows] = await db.query(
    `SELECT s.id, s.nombre, s.latitud, s.longitud, s.activo,
            l.temperatura, l.humedad, l.creado_en AS ultima_lectura
       FROM sensores s
       LEFT JOIN lecturas_sensor l ON l.id = (
         SELECT MAX(id) FROM lecturas_sensor WHERE sensor_id = s.id
       )
      WHERE s.proveedor_id = ?
      ORDER BY s.id DESC`,
    [req.proveedorId]
  );
  res.json(rows);
}));

// GET /sensores/:id/lecturas?limite=50  -> historial
router.get('/:id/lecturas', auth, soloRol('proveedor'), proveedorActual, manejar(async (req, res) => {
  const [s] = await db.query(
    'SELECT id FROM sensores WHERE id = ? AND proveedor_id = ?',
    [req.params.id, req.proveedorId]
  );
  if (s.length === 0) return res.status(404).json({ error: 'Sensor no encontrado' });

  const limite = Math.min(Math.max(parseInt(req.query.limite, 10) || 50, 1), 500);
  const [rows] = await db.query(
    'SELECT temperatura, humedad, creado_en FROM lecturas_sensor WHERE sensor_id = ? ORDER BY id DESC LIMIT ?',
    [req.params.id, limite]
  );
  res.json(rows);
}));

module.exports = router;