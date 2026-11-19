CREATE DATABASE IF NOT EXISTS metl CHARACTER SET utf8mb4;
USE metl;

CREATE TABLE IF NOT EXISTS usuarios (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  correo VARCHAR(150) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  rol ENUM('cliente','proveedor') NOT NULL,
  telefono VARCHAR(30) NULL,
  creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS proveedores (
  id INT AUTO_INCREMENT PRIMARY KEY,
  usuario_id INT NOT NULL UNIQUE,
  nombre_negocio VARCHAR(150) NOT NULL,
  descripcion TEXT NULL,
  municipio VARCHAR(100) NOT NULL,
  latitud DECIMAL(9,6) NOT NULL,
  longitud DECIMAL(9,6) NOT NULL,
  radio_km INT DEFAULT 10,
  FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE TABLE IF NOT EXISTS parcelas (
  id INT AUTO_INCREMENT PRIMARY KEY,
  proveedor_id INT NOT NULL,
  nombre VARCHAR(100) NOT NULL,
  municipio VARCHAR(100) NOT NULL,
  FOREIGN KEY (proveedor_id) REFERENCES proveedores(id)
);

CREATE TABLE IF NOT EXISTS magueyes (
  id INT AUTO_INCREMENT PRIMARY KEY,
  parcela_id INT NOT NULL,
  codigo VARCHAR(20) NOT NULL UNIQUE,
  variedad VARCHAR(60),
  edad_anios INT,
  ubicacion VARCHAR(150),
  humedad DECIMAL(5,2) NULL,
  temperatura DECIMAL(5,2) NULL,
  estado ENUM('capar_pronto','riego_bajo','en_desarrollo') DEFAULT 'en_desarrollo',
  nota VARCHAR(200) NULL,
  FOREIGN KEY (parcela_id) REFERENCES parcelas(id)
);

CREATE TABLE IF NOT EXISTS recomendaciones (
  id INT AUTO_INCREMENT PRIMARY KEY,
  maguey_id INT NOT NULL,
  titulo VARCHAR(200) NOT NULL,
  confianza INT NOT NULL,
  detalle VARCHAR(300) NULL,
  FOREIGN KEY (maguey_id) REFERENCES magueyes(id)
);

CREATE TABLE IF NOT EXISTS lotes (
  id INT AUTO_INCREMENT PRIMARY KEY,
  proveedor_id INT NOT NULL,
  parcela_id INT NULL,
  codigo VARCHAR(30) NOT NULL UNIQUE,
  creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (proveedor_id) REFERENCES proveedores(id),
  FOREIGN KEY (parcela_id) REFERENCES parcelas(id)
);

CREATE TABLE IF NOT EXISTS lote_pasos (
  id INT AUTO_INCREMENT PRIMARY KEY,
  lote_id INT NOT NULL,
  orden INT NOT NULL,
  titulo VARCHAR(100) NOT NULL,
  detalle VARCHAR(200) NULL,
  fecha DATE NULL,
  FOREIGN KEY (lote_id) REFERENCES lotes(id)
);

CREATE TABLE IF NOT EXISTS pedidos (
  id INT AUTO_INCREMENT PRIMARY KEY,
  cliente_id INT NOT NULL,
  proveedor_id INT NOT NULL,
  estado ENUM('pendiente','aceptado','rechazado','entregado') DEFAULT 'pendiente',
  nota VARCHAR(200) NULL,
  creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (cliente_id) REFERENCES usuarios(id),
  FOREIGN KEY (proveedor_id) REFERENCES proveedores(id)
);

CREATE TABLE IF NOT EXISTS productos (
  id INT AUTO_INCREMENT PRIMARY KEY,
  proveedor_id INT NOT NULL,
  nombre VARCHAR(120) NOT NULL,
  descripcion VARCHAR(250) NULL,
  precio DECIMAL(10,2) NOT NULL,
  stock DECIMAL(10,2) NOT NULL DEFAULT 0,
  activo TINYINT(1) DEFAULT 1,
  FOREIGN KEY (proveedor_id) REFERENCES proveedores(id)
);
