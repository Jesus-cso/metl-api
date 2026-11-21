const express = require('express');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const db = require('../db');
const { auth, soloRol, proveedorActual } = require('../middleware/auth');

const router = express.Router();

const dirUploads = path.join(__dirname, '..', 'uploads');
fs.mkdirSync(dirUploads, { recursive: true });

const upload = multer({
  storage: multer.diskStorage({
    destination: dirUploads,
    filename: (req, file, cb) =>
      cb(null, Date.now() + '-' + Math.round(Math.random() * 1e6) + path.extname(file.originalname).toLowerCase()),
  }),
  limits: { fileSize: 5 * 1024 * 1024 },
  fileFilter: (req, file, cb) => cb(null, /^image\/(jpeg|png|webp)$/.test(file.mimetype)),
});

// POST /productos/:id/foto  (multipart, campo "foto")
router.post('/:id/foto', auth, soloRol('proveedor'), proveedorActual, upload.single('foto'), async (req, res) => {
  if (!req.file) return res.status(400).json({ error: 'Sube una imagen JPG, PNG o WEBP (máx. 5 MB)' });

  const [rows] = await db.query(
    'SELECT id FROM productos WHERE id = ? AND proveedor_id = ?',
    [req.params.id, req.proveedorId]
  );
  if (rows.length === 0) {
    fs.unlink(req.file.path, () => {});
    return res.status(404).json({ error: 'Producto no encontrado' });
  }

  const url = '/uploads/' + req.file.filename;
  await db.query('UPDATE productos SET foto_url = ? WHERE id = ?', [url, req.params.id]);
  res.json({ foto_url: url });
});

module.exports = router;