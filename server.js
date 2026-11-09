require('dotenv').config();
const express = require('express');
const cors = require('cors');

if (!process.env.JWT_SECRET || process.env.JWT_SECRET.length < 16) {
  console.error('Falta JWT_SECRET en el archivo .env (mínimo 16 caracteres)');
  process.exit(1);
}

const app = express();
app.disable('x-powered-by');
app.use(cors());
app.use(express.json({ limit: '100kb' }));

app.get('/salud', (req, res) => res.json({ ok: true }));

app.use('/auth', require('./routes/auth'));
app.use('/proveedor', require('./routes/proveedor'));
app.use('/lotes', require('./routes/lotes'));

app.use((req, res) => res.status(404).json({ error: 'Ruta no encontrada' }));

// Cualquier error no controlado llega aquí (Express 5 también captura los async)
app.use((err, req, res, next) => {
  console.error(err);
  res.status(500).json({ error: 'Error interno del servidor' });
});

const PORT = Number(process.env.PORT || 3000);
app.listen(PORT, '0.0.0.0', () => {
  console.log(`API de Metl escuchando en http://localhost:${PORT}`);
});