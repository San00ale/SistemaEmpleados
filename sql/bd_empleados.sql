-- ============================================================
-- BASE DE DATOS: Sistema de Registro de Empleados
-- Universidad Técnica de Ambato
-- Motor: MySQL 5.7+ / XAMPP / phpMyAdmin
-- Convención de campos: prefijo del nombre de tabla
--   emp = empleados, usu = usuarios, asi = asistencias
-- ============================================================

DROP DATABASE IF EXISTS bd_empleados;
CREATE DATABASE bd_empleados
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE bd_empleados;

-- ============================================================
-- TABLA: usuarios
-- ============================================================
CREATE TABLE usuarios (
    usuId          INT           NOT NULL AUTO_INCREMENT,
    usuUsername    VARCHAR(50)   NOT NULL UNIQUE,
    usuPassword    VARCHAR(255)  NOT NULL,   -- SHA-256 en hex
    usuPerfil      ENUM('ADMIN','EMPLEADO') NOT NULL DEFAULT 'EMPLEADO',
    usuActivo      TINYINT(1)    NOT NULL DEFAULT 1,
    PRIMARY KEY (usuId)
) ENGINE=InnoDB COMMENT='Cuentas de acceso al sistema';

-- ============================================================
-- TABLA: empleados
-- cedula: CHAR(10), solo dígitos, exactamente 10, validado en BD y en Java
-- ============================================================
CREATE TABLE empleados (
    empId           INT          NOT NULL AUTO_INCREMENT,
    empCedula       CHAR(10)     NOT NULL UNIQUE
                    COMMENT 'Cédula ecuatoriana: exactamente 10 dígitos numéricos',
    empNombres      VARCHAR(100) NOT NULL,
    empApellidos    VARCHAR(100) NOT NULL,
    empCorreo       VARCHAR(150)          DEFAULT NULL,
    empTelefono     VARCHAR(15)           DEFAULT NULL,
    empCargo        VARCHAR(100)          DEFAULT NULL,
    empTipoJornada  ENUM('COMPLETO','PARCIAL') NOT NULL
                    COMMENT 'COMPLETO=sueldo fijo $1500/mes | PARCIAL=$5/hora max 8h/dia',
    empFechaIngreso DATE         NOT NULL,
    empActivo       TINYINT(1)   NOT NULL DEFAULT 1,
    empIdUsuario    INT                   DEFAULT NULL,
    empTieneFoto    TINYINT(1)   NOT NULL DEFAULT 0,
    empFotoRostro   LONGBLOB              DEFAULT NULL
                    COMMENT 'Foto JPEG del rostro para reconocimiento facial',
    PRIMARY KEY (empId),
    -- Validar exactamente 10 dígitos numéricos a nivel de BD
    CONSTRAINT chk_cedula_longitud CHECK (CHAR_LENGTH(empCedula) = 10),
    CONSTRAINT chk_cedula_numerica CHECK (empCedula REGEXP '^[0-9]{10}$'),
    CONSTRAINT fk_emp_usuario FOREIGN KEY (empIdUsuario)
        REFERENCES usuarios(usuId) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Empleados de la empresa';

-- ============================================================
-- TABLA: asistencias
-- Una sola fila por empleado-día (UNIQUE empIdEmpleado + asiiFecha)
-- El botón "Registrar Asistencia" registra la hora actual:
--   - Primera pulsación del día → graba asiEntradaManana
--   - Segunda pulsación         → graba asiSalidaManana  (si < 14:00)
--                                  o asiEntradaTarde     (si >= 14:00)
--   - Tercera/cuarta            → completa las horas restantes
-- ============================================================
CREATE TABLE asistencias (
    asiId             INT           NOT NULL AUTO_INCREMENT,
    asiIdEmpleado     INT           NOT NULL,
    asiFecha          DATE          NOT NULL,
    -- Jornada Mañana  08:00 – 13:00
    asiEntradaManana  TIME                   DEFAULT NULL,
    asiSalidaManana   TIME                   DEFAULT NULL,
    -- Jornada Tarde   14:00 – 17:00
    asiEntradaTarde   TIME                   DEFAULT NULL,
    asiSalidaTarde    TIME                   DEFAULT NULL,
    -- Minutos de atraso acumulados (entrada tarde > 08:00 | > 14:00)
    asiAtrasoMinutos  INT           NOT NULL DEFAULT 0,
    -- Minutos de salida anticipada (salida < 13:00 | < 17:00)
    asiSalidaAntMinutos INT         NOT NULL DEFAULT 0,
    -- Total minutos descontados = atraso + salida anticipada
    asiTotalDescMinutos INT         NOT NULL DEFAULT 0,
    -- Horas trabajadas efectivas (decimal con minutos ajustados)
    asiHorasTrabajadas DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    asiObservacion    VARCHAR(255)           DEFAULT NULL,
    PRIMARY KEY (asiId),
    -- Restricción: un solo registro por empleado por día
    UNIQUE KEY uk_asistencia_dia (asiIdEmpleado, asiFecha),
    CONSTRAINT fk_asi_emp FOREIGN KEY (asiIdEmpleado)
        REFERENCES empleados(empId) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Control de asistencia diaria';

-- ============================================================
-- ÍNDICES
-- ============================================================
CREATE INDEX idx_asi_fecha     ON asistencias(asiFecha);
CREATE INDEX idx_asi_emp_fecha ON asistencias(asiIdEmpleado, asiFecha);
CREATE INDEX idx_emp_cedula    ON empleados(empCedula);
CREATE INDEX idx_emp_tipo      ON empleados(empTipoJornada);
CREATE INDEX idx_emp_foto      ON empleados(empTieneFoto);

-- ============================================================
-- VISTAS
-- ============================================================

-- Reporte mensual: Tiempo Completo
CREATE OR REPLACE VIEW v_reporte_completo AS
SELECT
    e.empCedula                                          AS cedula,
    CONCAT(e.empNombres,' ',e.empApellidos)              AS nombre_completo,
    e.empCargo                                           AS cargo,
    a.asiFecha                                           AS fecha,
    a.asiEntradaManana                                   AS entrada_manana,
    a.asiSalidaManana                                    AS salida_manana,
    a.asiEntradaTarde                                    AS entrada_tarde,
    a.asiSalidaTarde                                     AS salida_tarde,
    a.asiAtrasoMinutos                                   AS atraso_minutos,
    a.asiSalidaAntMinutos                                AS salida_ant_minutos,
    a.asiTotalDescMinutos                                AS total_desc_minutos,
    ROUND(a.asiTotalDescMinutos * 0.20, 2)               AS total_descuento,
    a.asiHorasTrabajadas                                 AS horas_trabajadas,
    1500.00                                              AS sueldo_base,
    ROUND(1500.00 - (a.asiTotalDescMinutos * 0.20), 2)  AS sueldo_neto
FROM empleados e
JOIN asistencias a ON e.empId = a.asiIdEmpleado
WHERE e.empTipoJornada = 'COMPLETO' AND e.empActivo = 1;

-- Reporte mensual: Tiempo Parcial
CREATE OR REPLACE VIEW v_reporte_parcial AS
SELECT
    e.empCedula                                          AS cedula,
    CONCAT(e.empNombres,' ',e.empApellidos)              AS nombre_completo,
    e.empCargo                                           AS cargo,
    a.asiFecha                                           AS fecha,
    a.asiEntradaManana                                   AS entrada_manana,
    a.asiSalidaManana                                    AS salida_manana,
    a.asiEntradaTarde                                    AS entrada_tarde,
    a.asiSalidaTarde                                     AS salida_tarde,
    a.asiHorasTrabajadas                                 AS horas_trabajadas,
    ROUND(a.asiHorasTrabajadas * 5.00, 2)               AS pago_dia
FROM empleados e
JOIN asistencias a ON e.empId = a.asiIdEmpleado
WHERE e.empTipoJornada = 'PARCIAL' AND e.empActivo = 1;

-- Vista empleados con estado foto (útil para administración)
CREATE OR REPLACE VIEW v_empleados_resumen AS
SELECT
    e.empId,
    e.empCedula,
    CONCAT(e.empNombres,' ',e.empApellidos) AS nombre_completo,
    e.empNombres,
    e.empApellidos,
    e.empCargo,
    e.empTipoJornada,
    e.empActivo,
    e.empTieneFoto,
    u.usuUsername,
    u.usuPerfil
FROM empleados e
LEFT JOIN usuarios u ON e.empIdUsuario = u.usuId
WHERE e.empActivo = 1
ORDER BY e.empApellidos, e.empNombres;

-- ============================================================
-- DATOS INICIALES
-- ============================================================

-- Administrador (password: admin123)
INSERT INTO usuarios (usuUsername, usuPassword, usuPerfil, usuActivo)
VALUES ('admin',
        '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
        'ADMIN', 1);

-- Empleados de prueba
INSERT INTO empleados (empCedula, empNombres, empApellidos, empCorreo,
                       empTelefono, empCargo, empTipoJornada, empFechaIngreso, empActivo)
VALUES
('1804567890','Juan Carlos','Pérez López',   'jperez@uta.edu.ec',  '0991234567','Docente',        'COMPLETO','2022-01-01',1),
('1802345678','María Elena','Salazar Torres','msalazar@uta.edu.ec', '0987654321','Administrativo', 'COMPLETO','2021-06-15',1),
('1805678901','Pedro',      'Mora Vega',     'pmora@uta.edu.ec',   '0993456789','Auxiliar',        'PARCIAL', '2023-03-10',1);

-- Cuenta de acceso para Juan Pérez (empleado de prueba, password: emp123)
INSERT INTO usuarios (usuUsername, usuPassword, usuPerfil, usuActivo)
VALUES ('jperez',
        '6b86b273ff34fce19d6b804eff5a3f5747ada4eaa22f1d49c01e52ddb7875b4b',
        'EMPLEADO', 1);

UPDATE empleados SET empIdUsuario = 2 WHERE empCedula = '1804567890';

-- ============================================================
-- VERIFICACIÓN FINAL
-- ============================================================
SELECT 'BD creada correctamente' AS estado;
SELECT empCedula, empNombres, empApellidos, empTipoJornada FROM empleados;
SELECT usuUsername, usuPerfil FROM usuarios;
