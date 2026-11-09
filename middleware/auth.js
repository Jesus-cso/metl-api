const jwt = require('jsonwebtoken');
const db = require('../db');

// Exige un token válido: Authorization: Bearer <token>
function auth(req, res, next) {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : null;
  if (!token) return res.status(401).json({ error: 'Falta el token' });
  try {
    req.user = jwt.verify(token, process.env.JWT_SECRET);
    next();
  } catch {
    res.status(401).json({ error: 'Token inválido o vencido' });
  }
}

// Solo deja pasar a ciertos roles
function soloRol(...roles) {
  return (req, res, next) => {
    if (!roles.includes(req.user.rol)) {
      return res.status(403).json({ error: 'No tienes permiso para esto' });
    }
    next();
  };
}

// Para rutas del proveedor: deja en req.proveedorId el id de su registro
async function proveedorActual(req, res, next) {
  const [rows] = await db.query('SELECT id FROM proveedores WHERE usuario_id = ?', [req.user.id]);
  if (rows.length === 0) {
    return res.status(404).json({ error: 'Este usuario no tiene perfil de proveedor' });
  }
  req.proveedorId = rows[0].id;
  next();
}

module.exports = { auth, soloRol, proveedorActual };