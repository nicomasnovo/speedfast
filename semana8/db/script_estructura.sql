CREATE DATABASE IF NOT EXISTS speedfast;

USE speedfast;

-- Se eliminan primero las tablas hijas para respetar las llaves foráneas,
-- de modo que el script pueda ejecutarse más de una vez sin errores.
DROP TABLE IF EXISTS entrega;
DROP TABLE IF EXISTS pedido;
DROP TABLE IF EXISTS repartidor;

CREATE TABLE repartidor (
id INT AUTO_INCREMENT PRIMARY KEY,
nombre VARCHAR(100) NOT NULL
);

CREATE TABLE pedido (
id INT AUTO_INCREMENT PRIMARY KEY,
direccion VARCHAR(150) NOT NULL,
tipo VARCHAR(30) NOT NULL, -- COMIDA | ENCOMIENDA | EXPRESS
estado VARCHAR(20) NOT NULL, -- PENDIENTE | EN_REPARTO | ENTREGADO
repartidor VARCHAR(100) NULL -- nombre del repartidor a cargo; NULL si aún no tiene
);

CREATE TABLE entrega (
id INT AUTO_INCREMENT PRIMARY KEY,
id_pedido INT NOT NULL,
id_repartidor INT NOT NULL,
fecha DATE NOT NULL,
hora TIME NOT NULL,
FOREIGN KEY (id_pedido) REFERENCES pedido(id),
FOREIGN KEY (id_repartidor) REFERENCES repartidor(id)
);

-- Datos de prueba iniciales
-- Son los mismos datos que antes se creaban desde el archivo Main
-- Main.cargarPedidosIniciales() y en Main.NOMBRES_REPARTIDORES.

-- Repartidores (se insertan primero por las llaves foráneas de entrega).
INSERT INTO repartidor (id, nombre) VALUES (1, 'Camila');
INSERT INTO repartidor (id, nombre) VALUES (2, 'Luis');
INSERT INTO repartidor (id, nombre) VALUES (3, 'Pedro');

-- Pedidos con que parte la simulación, todos en estado PENDIENTE.
INSERT INTO pedido (id, direccion, tipo, estado)
VALUES (101, 'Av. Italia 456', 'COMIDA', 'PENDIENTE');
INSERT INTO pedido (id, direccion, tipo, estado)
VALUES (102, 'Av. Apoquindo 1500', 'ENCOMIENDA', 'PENDIENTE');
INSERT INTO pedido (id, direccion, tipo, estado)
VALUES (103, 'Av. Santa Rosa 567', 'EXPRESS', 'PENDIENTE');
INSERT INTO pedido (id, direccion, tipo, estado)
VALUES (104, 'Los Leones 2100', 'COMIDA', 'PENDIENTE');
INSERT INTO pedido (id, direccion, tipo, estado)
VALUES (105, 'Providencia 890', 'ENCOMIENDA', 'PENDIENTE');
INSERT INTO pedido (id, direccion, tipo, estado)
VALUES (106, 'San Pablo 3200', 'EXPRESS', 'PENDIENTE');
