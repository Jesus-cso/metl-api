const express = require('express');
const db = require('../db');
const { auth, soloRol, proveedorActual } = require('../middleware/auth');

const router = express.Router();
const fallo = (status, msg) => Object.assign(new Error(msg), { status });

// Junta los productos de cada pedido
async function conItems(pedidos) {
  if (pedidos.length === 0) return pedidos;
  const [items] = await db.query(
    `SELECT i.pedido_id, i.producto_id, p.nombre, i.cantidad, i.precio_unit
       FROM pedido_items i JOIN productos p ON p.id = i.producto_id
      WHERE i.pedido_id IN (?)`,
    [pedidos.map((p) => p.id)]
  );
  return pedidos.map((p) => ({ ...p, items: items.filter((i) => i.pedido_id === p.id) }));
}

// POST /pedidos  (cliente) { proveedor_id, nota, items: [{ producto_id, cantidad }] }
router.post('/', auth, soloRol('cliente'), async (req, res) => {
  const { proveedor_id, nota, items } = req.body || {};
  if (!proveedor_id || !Array.isArray(items) || items.length === 0) {
    return res.status(400).json({ error: 'Faltan datos: proveedor_id e items' });
  }
  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();
    const lineas = [];
    for (const it of items) {
      const cantidad = Number(it.cantidad);
      if (!it.producto_id || !(cantidad > 0)) throw fallo(400, 'Cada item necesita producto_id y cantidad mayor a 0');
      const [p] = await conn.query(
        'SELECT id, precio, stock, activo FROM productos WHERE id = ? AND proveedor_id = ?',
        [it.producto_id, proveedor_id]
      );
      if (p.length === 0) throw fallo(400, `El producto ${it.producto_id} no es de ese proveedor`);
      if (!p[0].activo || Number(p[0].stock) < cantidad) throw fallo(409, `Sin stock suficiente del producto ${it.producto_id}`);
      lineas.push({ id: p[0].id, cantidad, precio: p[0].precio });
    }
    const [r] = await conn.query(
      'INSERT INTO pedidos (cliente_id, proveedor_id, estado, nota) VALUES (?, ?, ?, ?)',
      [req.user.id, proveedor_id, 'pendiente', nota || null]
    );
    for (const l of lineas) {
      await conn.query(
        'INSERT INTO pedido_items (pedido_id, producto_id, cantidad, precio_unit) VALUES (?, ?, ?, ?)',
        [r.insertId, l.id, l.cantidad, l.precio]
      );
    }
    await conn.commit();
    res.status(201).json({ id: r.insertId, estado: 'pendiente' });
  } catch (e) {
    await conn.rollback();
    if (e.status) return res.status(e.status).json({ error: e.message });
    throw e;
  } finally {
    conn.release();
  }
});

// GET /pedidos/mios  (cliente)
router.get('/mios', auth, soloRol('cliente'), async (req, res) => {
  const [rows] = await db.query(
    `SELECT p.id, p.estado, p.nota, p.creado_en, pr.nombre_negocio AS proveedor
       FROM pedidos p JOIN proveedores pr ON pr.id = p.proveedor_id
      WHERE p.cliente_id = ? ORDER BY p.id DESC`,
    [req.user.id]
  );
  res.json(await conItems(rows));
});

// GET /pedidos/recibidos  (proveedor)
router.get('/recibidos', auth, soloRol('proveedor'), proveedorActual, async (req, res) => {
  const [rows] = await db.query(
    `SELECT p.id, p.estado, p.nota, p.creado_en, u.nombre AS cliente
       FROM pedidos p JOIN usuarios u ON u.id = p.cliente_id
      WHERE p.proveedor_id = ? ORDER BY p.id DESC`,
    [req.proveedorId]
  );
  res.json(await conItems(rows));
});

// PATCH /pedidos/:id/estado  (proveedor) { estado: aceptado | rechazado | entregado }
const PERMITIDOS = { pendiente: ['aceptado', 'rechazado'], aceptado: ['entregado'] };

router.post('/:id/estado', auth, soloRol('proveedor'), proveedorActual, async (req, res) => {
  const { estado } = req.body || {};
  if (!['aceptado', 'rechazado', 'entregado'].includes(estado)) {
    return res.status(400).json({ error: 'estado debe ser aceptado, rechazado o entregado' });
  }
  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();
    const [rows] = await conn.query(
      'SELECT id, estado FROM pedidos WHERE id = ? AND proveedor_id = ? FOR UPDATE',
      [req.params.id, req.proveedorId]
    );
    if (rows.length === 0) throw fallo(404, 'Pedido no encontrado');
    if (!(PERMITIDOS[rows[0].estado] || []).includes(estado)) {
      throw fallo(409, `No se puede pasar de ${rows[0].estado} a ${estado}`);
    }
    if (estado === 'aceptado') {
      const [items] = await conn.query('SELECT producto_id, cantidad FROM pedido_items WHERE pedido_id = ?', [rows[0].id]);
      for (const it of items) {
        const [u] = await conn.query(
          'UPDATE productos SET stock = stock - ? WHERE id = ? AND stock >= ?',
          [it.cantidad, it.producto_id, it.cantidad]
        );
        if (u.affectedRows === 0) throw fallo(409, 'Stock insuficiente para aceptar este pedido');
      }
    }
    await conn.query('UPDATE pedidos SET estado = ? WHERE id = ?', [estado, rows[0].id]);
    await conn.commit();
    res.json({ id: rows[0].id, estado });
  } catch (e) {
    await conn.rollback();
    if (e.status) return res.status(e.status).json({ error: e.message });
    throw e;
  } finally {
    conn.release();
  }
});

// POST /pedidos/:id/cancelar (cliente) - solo si sigue pendiente
router.post('/:id/cancelar', auth, soloRol('cliente'), async (req, res) => {
  const [r] = await db.query(
    "UPDATE pedidos SET estado = 'cancelado' WHERE id = ? AND cliente_id = ? AND estado = 'pendiente'",
    [req.params.id, req.user.id]
  );
  if (r.affectedRows === 0) {
    return res.status(409).json({ error: 'El pedido no existe o ya no se puede cancelar' });
  }
  res.json({ id: Number(req.params.id), estado: 'cancelado' });
});
module.exports = router;

