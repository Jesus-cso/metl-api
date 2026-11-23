const express = require('express');
const crypto = require('crypto');
const db = require('../db');
const { auth, soloRol, proveedorActual } = require('../middleware/auth');

const router = express.Router();

// El ESP32 se identifica con el header x-device-key
function claveValida(req, res, next) {
  const esperada = Buffer.from(process.env.DEVICE_KEY || '');
  const recibida = Buffer.from(String(req.headers['x-device-key'] || ''));
  if (esperada.length < 16 || esperada.length !== recibida.length ||
      !crypto.timingSafeEqual(esperada, recibida)) {
    return res.status(401).json({ error: 'Clave de dispositivo invalida' });
  }
  next();
}

// POST /sensores/lecturas  { codigo, humedad, temperatura }
router.post('/lecturas', claveValida, async (req, res) => {
  const { codigo, humedad, temperatura } = req.body || {};
  const h = humedad == null ? null : Number(humedad);
  const t = temperatura == null ? null : Number(temperatura);

  if (!codigo || typeof codigo !== 'string') {
    return res.status(400).json({ error: 'Falta el codigo del maguey' });
  }
  if ((h === null && t === null) ||
      (h !== null && !(h >= 0 && h <= 100)) ||
      (t !== null && !(t >= -40 && t <= 85))) {
    return res.status(400).json({ error: 'Lectura fuera de rango o vacia' });
  }

  const [m] = await db.query('SELECT id FROM magueyes WHERE codigo = ?', [codigo]);
  if (m.length === 0) return res.status(404).json({ error: 'Maguey no encontrado' });

  await db.query('INSERT INTO lecturas (maguey_id, humedad, temperatura) VALUES (?, ?, ?)', [m[0].id, h, t]);
  await db.query(
    "UPDATE magueyes SET humedad = COALESCE(?, humedad), temperatura = COALESCE(?, temperatura), estado = CASE WHEN COALESCE(?, humedad) < 30 THEN 'riego_bajo' WHEN estado = 'riego_bajo' THEN 'en_desarrollo' ELSE estado END WHERE id = ?",
    [h, t, h, m[0].id]
  );
  res.status(201).json({ ok: true });
});

// GET /sensores/magueyes/:codigo/lecturas?limite=50  (proveedor, solo sus magueyes)
router.get('/magueyes/:codigo/lecturas', auth, soloRol('proveedor'), proveedorActual, async (req, res) => {
  const limite = Math.min(Math.max(parseInt(req.query.limite, 10) || 50, 1), 500);
  const [rows] = await db.query(
    `SELECT l.humedad, l.temperatura, l.creado_en
       FROM lecturas l
       JOIN magueyes m ON m.id = l.maguey_id
       JOIN parcelas p ON p.id = m.parcela_id
      WHERE p.proveedor_id = ? AND m.codigo = ?
      ORDER BY l.id DESC LIMIT ?`,
    [req.proveedorId, req.params.codigo, limite]
  );
  res.json(rows.reverse());
});

module.exports = router;
