const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const db = require('../db');

const router = express.Router();

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

// Ubicación por defecto (Tulancingo) si la app aún no manda datos del negocio
const NEGOCIO_DEFAULT = {
  municipio: 'Tulancingo',
  latitud: 20.0833,
  longitud: -98.3683,
  radio_km: 10,
};

function firmar(usuario) {
  return jwt.sign({ id: usuario.id, rol: usuario.rol }, process.env.JWT_SECRET, {
    expiresIn: '30d',
  });
}

// POST /auth/registro
router.post('/registro', async (req, res) => {
  const { nombre, correo, password, rol, telefono, negocio } = req.body || {};

  if (!nombre || !correo || !password || !['cliente', 'proveedor'].includes(rol)) {
    return res
      .status(400)
      .json({ error: 'Faltan datos: nombre, correo, password y rol (cliente o proveedor)' });
  }
  if (!EMAIL_RE.test(correo)) {
    return res.status(400).json({ error: 'Correo no válido' });
  }
  if (String(password).length < 6) {
    return res.status(400).json({ error: 'La contraseña debe tener al menos 6 caracteres' });
  }

  // Datos del negocio (solo proveedores), con valores por defecto
  let n = null;
  if (rol === 'proveedor') {
    const src = negocio || {};
    n = {
      nombre_negocio: (src.nombre_negocio || nombre).trim(),
      descripcion: src.descripcion || null,
      municipio: src.municipio || NEGOCIO_DEFAULT.municipio,
      latitud: Number(src.latitud ?? NEGOCIO_DEFAULT.latitud),
      longitud: Number(src.longitud ?? NEGOCIO_DEFAULT.longitud),
      radio_km: Number(src.radio_km) || NEGOCIO_DEFAULT.radio_km,
    };
    if (!Number.isFinite(n.latitud) || !Number.isFinite(n.longitud)) {
      return res.status(400).json({ error: 'Latitud o longitud no válidas' });
    }
  }

  const hash = await bcrypt.hash(String(password), 10);
  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();

    const [r] = await conn.query(
      'INSERT INTO usuarios (nombre, correo, password_hash, rol, telefono) VALUES (?, ?, ?, ?, ?)',
      [nombre.trim(), correo.trim().toLowerCase(), hash, rol, telefono || null]
    );

    if (rol === 'proveedor') {
      await conn.query(
        `INSERT INTO proveedores
           (usuario_id, nombre_negocio, descripcion, municipio, latitud, longitud, radio_km)
         VALUES (?, ?, ?, ?, ?, ?, ?)`,
        [r.insertId, n.nombre_negocio, n.descripcion, n.municipio, n.latitud, n.longitud, n.radio_km]
      );
    }

    await conn.commit();
    const usuario = { id: r.insertId, nombre: nombre.trim(), rol };
    res.status(201).json({ token: firmar(usuario), usuario });
  } catch (e) {
    await conn.rollback();
    if (e.code === 'ER_DUP_ENTRY') {
      return res.status(409).json({ error: 'Ese correo ya está registrado' });
    }
    console.error('REGISTRO ERROR:', e.code, e.sqlMessage || e.message);
    res.status(500).json({ error: e.sqlMessage || e.message });
  } finally {
    conn.release();
  }
});

// POST /auth/login
router.post('/login', async (req, res) => {
  const { correo, password } = req.body || {};
  if (!correo || !password) {
    return res.status(400).json({ error: 'Escribe tu correo y contraseña' });
  }
  try {
    const [rows] = await db.query(
      'SELECT id, nombre, password_hash, rol FROM usuarios WHERE correo = ?',
      [String(correo).trim().toLowerCase()]
    );
    const u = rows[0];
    if (!u || !(await bcrypt.compare(String(password), u.password_hash))) {
      return res.status(401).json({ error: 'Correo o contraseña incorrectos' });
    }
    const usuario = { id: u.id, nombre: u.nombre, rol: u.rol };
    res.json({ token: firmar(usuario), usuario });
  } catch (e) {
    console.error('LOGIN ERROR:', e.code, e.sqlMessage || e.message);
    res.status(500).json({ error: e.sqlMessage || e.message });
  }
});

module.exports = router;