USE metl;
ALTER TABLE productos ADD COLUMN IF NOT EXISTS foto_url VARCHAR(255) NULL;
ALTER TABLE pedidos MODIFY estado ENUM('pendiente','aceptado','rechazado','entregado','cancelado') DEFAULT 'pendiente';
