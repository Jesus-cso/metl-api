const express = require('express');
const db = require('../db');
const { auth, soloRol, proveedorActual } = require('../middleware/auth');

const router = express.Router();

// GET /productos/proveedor  -> Ver los productos del proveedor autenticado
router.get('/proveedor', auth, soloRol('proveedor'), proveedorActual, async (req, res) => {
  const [rows] = await db.query(
    'SELECT id, nombre, descripcion, precio, stock, activo FROM productos WHERE proveedor_id = ? ORDER BY id DESC',
    [req.proveedorId]
  );
  res.json(rows);
});

// POST /productos  -> Crear un producto nuevo
router.post('/', auth, soloRol('proveedor'), proveedorActual, async (req, res) => {
  const { nombre, descripcion, precio, stock } = req.body || {};
  if (!nombre || precio === undefined || stock === undefined) {
    return res.status(400).json({ error: 'Faltan datos obligatorios: nombre, precio y stock' });
  }

  const [r] = await db.query(
    `INSERT INTO productos (proveedor_id, nombre, descripcion, precio, stock, activo)
     VALUES (?, ?, ?, ?, ?, 1)`,
    [req.proveedorId, nombre.trim(), descripcion || null, Number(precio), Number(stock)]
  );

  res.status(201).json({ id: r.insertId, mensaje: 'Producto creado exitosamente' });
});

// PUT /productos/:id  -> Actualizar un producto
router.put('/:id', auth, soloRol('proveedor'), proveedorActual, async (req, res) => {
  const { nombre, descripcion, precio, stock, activo } = req.body || {};
  
  const [rows] = await db.query('SELECT id FROM productos WHERE id = ? AND proveedor_id = ?', [req.params.id, req.proveedorId]);
  if (rows.length === 0) return res.status(404).json({ error: 'Producto no encontrado' });

  await db.query(
    `UPDATE productos 
        SET nombre = COALESCE(?, nombre),
            descripcion = COALESCE(?, descripcion),
            precio = COALESCE(?, precio),
            stock = COALESCE(?, stock),
            activo = COALESCE(?, activo)
      WHERE id = ?`,
    [nombre, descripcion, precio !== undefined ? Number(precio) : undefined, stock !== undefined ? Number(stock) : undefined, activo !== undefined ? Boolean(activo) : undefined, req.params.id]
  );

  res.json({ mensaje: 'Producto actualizado' });
});

// GET /productos/publicos  -> Catálogo público para los clientes
router.get('/publicos', auth, soloRol('cliente'), async (req, res) => {
  const [rows] = await db.query(
    `SELECT p.id, p.nombre, p.descripcion, p.precio, p.stock, pr.nombre_negocio AS proveedor, pr.municipio
       FROM productos p
       JOIN proveedores pr ON pr.id = p.proveedor_id
      WHERE p.activo = 1 AND p.stock > 0
      ORDER BY p.id DESC`
  );
  res.json(rows);
});

module.exports = router;