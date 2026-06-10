-- ============================================================
-- OPCIONAL — Solo si ejecutó una versión ANTIGUA de
-- actualizacion_facial.sql que creó foto_rostro / tiene_foto
-- ============================================================

USE bd_empleados;

-- Copiar datos de columnas viejas a las correctas
UPDATE empleados
SET empFotoRostro = foto_rostro,
    empTieneFoto  = COALESCE(tiene_foto, 0)
WHERE foto_rostro IS NOT NULL;

-- Quitar columnas incorrectas (ejecutar una vez)
-- ALTER TABLE empleados DROP COLUMN foto_rostro, DROP COLUMN tiene_foto;

SELECT 'Migración legacy aplicada' AS estado;
