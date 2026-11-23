const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const db = require('../db');

const router = express.Router();

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function firmar(usuario) {
  return jwt.sign({ id: usuario.id, rol: usuario.rol }, process.env.JWT_SECRET, {
    expiresIn: '7d',
  });
}

// POST /auth/registro
router.post('/registro', async (req, res) => {
  const { nombre, correo, password, rol, telefono, negocio } = req.body || {};

  if (!nombre || !correo || !password || !['cliente', 'proveedor'].includes(rol)) {
    return res.status(400).json({ error: 'Faltan datos: nombre, correo, password y rol (cliente o proveedor)' });
  }
  if (!EMAIL_RE.test(correo)) {
    return res.status(400).json({ error: 'Correo no válido' });
  }
  if (String(password).length < 8) {
    return res.status(400).json({ error: 'La contraseña debe tener al menos 8 caracteres' });
  }

  if (rol === 'proveedor') {
    const lat = Number(negocio?.latitud);
    const lng = Number(negocio?.longitud);
    if (!negocio?.nombre_negocio || !negocio?.municipio ||
        !Number.isFinite(lat) || !Number.isFinite(lng) ||
        lat < -90 || lat > 90 || lng < -180 || lng > 180) {
      return res.status(400).json({
        error: 'Un proveedor necesita negocio: nombre_negocio, municipio, latitud y longitud válidas',
      });
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
        `INSERT INTO proveedores (usuario_id, nombre_negocio, descripcion, municipio, latitud, longitud, radio_km)
         VALUES (?, ?, ?, ?, ?, ?, ?)`,
        [r.insertId, negocio.nombre_negocio, negocio.descripcion || null, negocio.municipio,
         Number(negocio.latitud), Number(negocio.longitud), Number(negocio.radio_km) || 10]
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
    throw e;
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
});

module.exports = router;