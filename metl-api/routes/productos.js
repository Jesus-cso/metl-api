const express = require('express');
const path = require('path');
const fs = require('fs');
const multer = require('multer');
const db = require('../db');
const { auth, soloRol, proveedorActual } = require('../middleware/auth');

const router = express.Router();

// ---- Subida de fotos ----
const carpetaUploads = path.join(__dirname, '..', 'uploads');
fs.mkdirSync(carpetaUploads, { recursive: true });

const upload = multer({
  storage: multer.diskStorage({
    destination: carpetaUploads,
    filename: (req, file, cb) =>
      cb(null, `prod-${req.params.id}-${Date.now()}.jpg`)
  }),
  limits: { fileSize: 5 * 1024 * 1024 },
  fileFilter: (req, file, cb) =>
    cb(null, /^image\//.test(file.mimetype))
});

// Captura errores de rutas async y los devuelve como JSON con detalle
const manejar = (fn) => (req, res) =>
  Promise.resolve(fn(req, res)).catch((err) => {
    console.error(`[${req.method} ${req.originalUrl}]`, err);
    res.status(500).json({ error: err.message || 'Error interno' });
  });

// GET /productos/proveedor
router.get('/proveedor', auth, soloRol('proveedor'), proveedorActual, manejar(async (req, res) => {
  const [rows] = await db.query(
    'SELECT id, nombre, descripcion, precio, stock, activo, foto_url FROM productos WHERE proveedor_id = ? ORDER BY id DESC',
    [req.proveedorId]
  );
  res.json(rows);
}));

// POST /productos
router.post('/', auth, soloRol('proveedor'), proveedorActual, manejar(async (req, res) => {
  const { nombre, descripcion, precio, stock } = req.body || {};

  if (req.proveedorId === undefined || req.proveedorId === null) {
    return res.status(403).json({ error: 'Tu usuario no tiene un perfil de proveedor' });
  }
  if (!nombre || precio === undefined || stock === undefined) {
    return res.status(400).json({ error: 'Faltan datos obligatorios: nombre, precio y stock' });
  }

  const [r] = await db.query(
    `INSERT INTO productos (proveedor_id, nombre, descripcion, precio, stock, activo)
     VALUES (?, ?, ?, ?, ?, 1)`,
    [req.proveedorId, String(nombre).trim(), descripcion || null, Number(precio), Number(stock)]
  );

  res.status(201).json({ id: r.insertId, mensaje: 'Producto creado exitosamente' });
}));

// POST /productos/:id/foto
router.post('/:id/foto', auth, soloRol('proveedor'), proveedorActual,
  async (req, res, next) => {
    // Verifica que el producto sea de este proveedor ANTES de guardar el archivo
    try {
      const [rows] = await db.query(
        'SELECT id FROM productos WHERE id = ? AND proveedor_id = ?',
        [req.params.id, req.proveedorId]
      );
      if (rows.length === 0) return res.status(404).json({ error: 'Producto no encontrado' });
      next();
    } catch (err) {
      console.error(err);
      res.status(500).json({ error: err.message });
    }
  },
  upload.single('foto'),
  manejar(async (req, res) => {
    if (!req.file) return res.status(400).json({ error: 'No llegó ninguna foto' });

    const fotoUrl = `/uploads/${req.file.filename}`;
    await db.query('UPDATE productos SET foto_url = ? WHERE id = ?', [fotoUrl, req.params.id]);

    res.json({ foto_url: fotoUrl, mensaje: 'Foto guardada' });
  })
);

// PUT /productos/:id
router.put('/:id', auth, soloRol('proveedor'), proveedorActual, manejar(async (req, res) => {
  const { nombre, descripcion, precio, stock, activo } = req.body || {};

  const [rows] = await db.query(
    'SELECT id FROM productos WHERE id = ? AND proveedor_id = ?',
    [req.params.id, req.proveedorId]
  );
  if (rows.length === 0) return res.status(404).json({ error: 'Producto no encontrado' });

  await db.query(
    `UPDATE productos
        SET nombre = COALESCE(?, nombre),
            descripcion = COALESCE(?, descripcion),
            precio = COALESCE(?, precio),
            stock = COALESCE(?, stock),
            activo = COALESCE(?, activo)
      WHERE id = ?`,
    [
      nombre ?? null,
      descripcion ?? null,
      precio !== undefined ? Number(precio) : null,
      stock !== undefined ? Number(stock) : null,
      activo !== undefined ? (activo ? 1 : 0) : null,
      req.params.id
    ]
  );

  res.json({ mensaje: 'Producto actualizado' });
}));

// GET /productos/publicos
router.get('/publicos', auth, soloRol('cliente'), manejar(async (req, res) => {
  const [rows] = await db.query(
    `SELECT p.id, p.nombre, p.descripcion, p.precio, p.stock, p.proveedor_id, p.foto_url,
            pr.nombre_negocio AS proveedor, pr.municipio
       FROM productos p
       JOIN proveedores pr ON pr.id = p.proveedor_id
      WHERE p.activo = 1 AND p.stock > 0
      ORDER BY p.id DESC`
  );
  res.json(rows);
}));

module.exports = router;