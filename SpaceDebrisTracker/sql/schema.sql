-- Crear la base de datos si no existe
CREATE DATABASE IF NOT EXISTS space_debris_db;
USE space_debris_db;

-- Tabla para almacenar los registros TLE crudos
CREATE TABLE IF NOT EXISTS space_debris (
    id INT AUTO_INCREMENT PRIMARY KEY,
    norad_cat_id INT UNIQUE NOT NULL,       -- ID único del satélite/escombro asignado por NORAD
    object_name VARCHAR(100) NOT NULL,      -- Nombre del objeto
    tle_line1 VARCHAR(100) NOT NULL,        -- Línea 1 del TLE
    tle_line2 VARCHAR(100) NOT NULL,        -- Línea 2 del TLE
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Historial de posiciones calculadas (la tabla y el mapa 2D leen de aquí)
CREATE TABLE IF NOT EXISTS orbital_positions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    norad_cat_id INT NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    altitude DOUBLE NOT NULL,               -- En kilómetros
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (norad_cat_id) REFERENCES space_debris(norad_cat_id) ON DELETE CASCADE
);

-- Las alertas de reentrada se eliminaron del alcance del proyecto.
-- Esta línea limpia la tabla en bases de datos creadas con la versión anterior.
DROP TABLE IF EXISTS reentry_alerts;
