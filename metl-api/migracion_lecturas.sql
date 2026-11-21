USE metl;
CREATE TABLE IF NOT EXISTS lecturas (
  id INT AUTO_INCREMENT PRIMARY KEY,
  maguey_id INT NOT NULL,
  humedad DECIMAL(5,2) NULL,
  temperatura DECIMAL(5,2) NULL,
  creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX (maguey_id, creado_en),
  FOREIGN KEY (maguey_id) REFERENCES magueyes(id)
);
