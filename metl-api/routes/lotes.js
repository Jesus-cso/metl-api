const express = require('express');
const db = require('../db');

const router = express.Router();

// GET /lotes/:codigo   (público: es lo que se abre al escanear el QR)
router.get('/:codigo', async (req, res) => {
  const [lotes] = await db.query(
    `SELECT l.id, l.codigo, l.creado_en, pr.nombre_negocio, pr.municipio, pa.nombre AS parcela
       FROM lotes l
       JOIN proveedores pr ON pr.id = l.proveedor_id
       LEFT JOIN parcelas pa ON pa.id = l.parcela_id
      WHERE l.codigo = ?`,
    [req.params.codigo]
  );
  if (lotes.length === 0) return res.status(404).json({ error: 'Lote no encontrado' });

  const [pasos] = await db.query(
    'SELECT titulo, detalle, fecha FROM lote_pasos WHERE lote_id = ? ORDER BY orden',
    [lotes[0].id]
  );
  const l = lotes[0];
  res.json({
    codigo: l.codigo,
    productor: l.nombre_negocio,
    municipio: l.municipio,
    parcela: l.parcela,
    creadoEn: l.creado_en,
    pasos,
  });
});

module.exports = router;