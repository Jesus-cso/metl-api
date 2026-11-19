const express = require('express');
const db = require('../db');
const { auth, soloRol } = require('../middleware/auth');

const router = express.Router();

// GET /proveedores/cercanos?municipio=Tulancingo
router.get('/cercanos', auth, soloRol('cliente'), async (req, res) => {
  const { municipio } = req.query;

  let query = `
    SELECT p.id, p.nombre_negocio, p.descripcion, p.municipio, p.latitud, p.longitud, p.radio_km,
           u.telefono, u.correo
      FROM proveedores p
      JOIN usuarios u ON u.id = p.usuario_id
  `;
  const params = [];

  if (municipio) {
    query += ' WHERE p.municipio LIKE ?';
    params.push(`%${municipio}%`);
  }

  query += ' ORDER BY p.id DESC LIMIT 20';

  const [rows] = await db.query(query, params);
  res.json(rows);
});

// GET /proveedores/:id/catalogo  -> Ver productos de un proveedor específico
router.get('/:id/catalogo', auth, soloRol('cliente'), async (req, res) => {
  const [proveedor] = await db.query('SELECT id, nombre_negocio, municipio FROM proveedores WHERE id = ?', [req.params.id]);
  if (proveedor.length === 0) return res.status(404).json({ error: 'Proveedor no encontrado' });

  const [productos] = await db.query(
    'SELECT id, nombre, descripcion, precio, stock FROM productos WHERE proveedor_id = ? AND activo = 1',
    [req.params.id]
  );

  res.json({
    proveedor: proveedor[0],
    productos
  });
});

module.exports = router;