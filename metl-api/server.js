try { require('dotenv').config(); } catch (_) { /* dotenv es opcional */ }

const express = require('express');
const path = require('path');
const fs = require('fs');

const app = express();

// ---- Middlewares generales ----
app.use(express.json({ limit: '1mb' }));

// Registro simple de peticiones (útil para depurar)
app.use((req, res, next) => {
  console.log(`${new Date().toLocaleTimeString()} ${req.method} ${req.originalUrl}`);
  next();
});

// ---- Fotos ----
const carpetaUploads = path.join(__dirname, 'uploads');
fs.mkdirSync(carpetaUploads, { recursive: true });
app.use('/uploads', express.static(carpetaUploads));

// ---- Rutas ----
app.get('/', (req, res) => res.json({ ok: true, servicio: 'Metl API' }));

app.use('/auth', require('./routes/auth'));            // <-- REVISA: login y registro
app.use('/productos', require('./routes/productos'));
app.use('/pedidos', require('./routes/pedidos'));      // <-- REVISA: si no existe, borra esta línea
app.use('/sensores', require('./routes/sensores'));

// ---- 404 ----
app.use((req, res) => {
  res.status(404).json({ error: `Ruta no encontrada: ${req.method} ${req.originalUrl}` });
});

// ---- Errores (multer, JSON mal formado, etc.) ----
app.use((err, req, res, next) => {
  console.error(`[${req.method} ${req.originalUrl}]`, err);
  if (err.code === 'LIMIT_FILE_SIZE') {
    return res.status(413).json({ error: 'La foto pesa más de 5 MB' });
  }
  if (err.type === 'entity.parse.failed') {
    return res.status(400).json({ error: 'JSON mal formado' });
  }
  res.status(err.status || 500).json({ error: err.message || 'Error interno' });
});

// ---- Arranque ----
const PORT = process.env.PORT || 3100;
app.listen(PORT, '0.0.0.0', () => {
  console.log(`API de Metl escuchando en el puerto ${PORT}`);
});