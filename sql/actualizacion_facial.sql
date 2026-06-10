-- ============================================================
-- ACTUALIZACIÓN BD — MÓDULO RECONOCIMIENTO FACIAL
-- Universidad Técnica de Ambato
-- Ejecutar en phpMyAdmin sobre la BD bd_empleados
--
-- IMPORTANTE: Los nombres de columna deben coincidir con Java
--   empFotoRostro, empTieneFoto  (NO foto_rostro / tiene_foto)
-- ============================================================

USE bd_empleados;

-- ── 1. Columnas en empleados (si la BD es anterior a bd_empleados.sql) ──
ALTER TABLE empleados
    ADD COLUMN IF NOT EXISTS empFotoRostro LONGBLOB DEFAULT NULL
        COMMENT 'Imagen JPEG del rostro (reconocimiento facial)',
    ADD COLUMN IF NOT EXISTS empTieneFoto TINYINT(1) NOT NULL DEFAULT 0
        COMMENT '1 = foto registrada, 0 = sin foto';

-- ── 2. Índice para login facial ───────────────────────────────
CREATE INDEX IF NOT EXISTS idx_emp_foto ON empleados(empTieneFoto);

-- ── 3. Vista administrativa (nombres emp / usu) ───────────────
CREATE OR REPLACE VIEW v_empleados_foto AS
SELECT
    e.empId,
    e.empCedula,
    CONCAT(e.empNombres, ' ', e.empApellidos) AS nombre_completo,
    e.empCargo,
    e.empTipoJornada,
    e.empActivo,
    e.empTieneFoto,
    CASE WHEN e.empFotoRostro IS NULL THEN 0
         ELSE OCTET_LENGTH(e.empFotoRostro) END AS bytes_foto,
    u.usuUsername,
    u.usuPerfil,
    e.empIdUsuario
FROM empleados e
LEFT JOIN usuarios u ON e.empIdUsuario = u.usuId
WHERE e.empActivo = 1
ORDER BY e.empApellidos, e.empNombres;

-- ── 4. Verificación rápida ────────────────────────────────────
SELECT 'empleados activos' AS concepto,
       COUNT(*) AS total,
       SUM(empTieneFoto) AS con_bandera_foto,
       SUM(CASE WHEN empFotoRostro IS NOT NULL AND OCTET_LENGTH(empFotoRostro) > 0 THEN 1 ELSE 0 END) AS con_blob_foto
FROM empleados
WHERE empActivo = 1;

SELECT e.empCedula,
       CONCAT(e.empNombres, ' ', e.empApellidos) AS nombre,
       e.empTieneFoto,
       e.empIdUsuario,
       u.usuUsername
FROM empleados e
LEFT JOIN usuarios u ON e.empIdUsuario = u.usuId
WHERE e.empActivo = 1
ORDER BY e.empApellidos;
