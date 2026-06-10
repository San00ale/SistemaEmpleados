package sistema.dao;

import sistema.model.Empleado;
import sistema.model.Usuario;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para reconocimiento facial.
 * Columnas: empFotoRostro (LONGBLOB), empTieneFoto (TINYINT).
 */
public class RostroDAO {

    /** Guarda la foto del rostro en BD. */
    public boolean guardarFoto(int empId, byte[] fotoBytes) throws Exception {
        if (fotoBytes == null || fotoBytes.length < 100) {
            throw new IllegalArgumentException(
                "La imagen capturada no es válida o es demasiado pequeña.");
        }
        String sql = "UPDATE empleados SET empFotoRostro=?, empTieneFoto=1 " +
                     "WHERE empId=? AND empActivo=1";
        try (Connection cn = Conexion.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setBytes(1, fotoBytes);
            ps.setInt(2, empId);
            int filas = ps.executeUpdate();
            if (filas == 0) {
                throw new SQLException(
                    "No se actualizó ningún empleado (empId=" + empId +
                    "). Verifique que exista y esté activo.");
            }
            return true;
        }
    }

    /** Obtiene la foto del rostro de un empleado. */
    public byte[] obtenerFoto(int empId) throws Exception {
        String sql = "SELECT empFotoRostro FROM empleados WHERE empId=? AND empTieneFoto=1";
        try (Connection cn = Conexion.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, empId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getBytes("empFotoRostro") : null;
        }
    }

    /** Lista empleados activos con foto (para reconocimiento en login). */
    public List<Empleado> listarEmpleadosConFoto() throws Exception {
        String sql = "SELECT empId, empCedula, empNombres, empApellidos, " +
                     "       empFotoRostro, empIdUsuario, empActivo, empTipoJornada, empCargo " +
                     "FROM empleados " +
                     "WHERE empTieneFoto=1 AND empActivo=1 " +
                     "  AND empFotoRostro IS NOT NULL AND OCTET_LENGTH(empFotoRostro) > 0";
        List<Empleado> lista = new ArrayList<>();
        try (Connection cn = Conexion.getConexion();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Empleado e = new Empleado();
                e.setEmpId(rs.getInt("empId"));
                e.setEmpCedula(rs.getString("empCedula"));
                e.setEmpNombres(rs.getString("empNombres"));
                e.setEmpApellidos(rs.getString("empApellidos"));
                e.setEmpFotoRostro(rs.getBytes("empFotoRostro"));
                e.setEmpTieneFoto(true);
                e.setEmpActivo(rs.getBoolean("empActivo"));
                int idu = rs.getInt("empIdUsuario");
                e.setEmpIdUsuario(rs.wasNull() ? null : idu);
                e.setEmpTipoJornada(Empleado.TipoJornada.valueOf(rs.getString("empTipoJornada")));
                e.setEmpCargo(rs.getString("empCargo"));
                lista.add(e);
            }
        }
        return lista;
    }

    /** Elimina la foto de un empleado. */
    public boolean eliminarFoto(int empId) throws Exception {
        String sql = "UPDATE empleados SET empFotoRostro=NULL, empTieneFoto=0 WHERE empId=?";
        try (Connection cn = Conexion.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, empId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Verifica si un empleado tiene foto. */
    public boolean tieneFoto(int empId) throws Exception {
        String sql = "SELECT empTieneFoto FROM empleados WHERE empId=?";
        try (Connection cn = Conexion.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, empId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getBoolean("empTieneFoto");
        }
    }

    /** Obtiene el usuario vinculado a un empleado (para construir sesión tras reconocimiento). */
    public Usuario obtenerUsuarioPorEmpleado(int empId) throws Exception {
        String sql = "SELECT u.usuId, u.usuUsername, u.usuPassword, u.usuPerfil, u.usuActivo " +
                     "FROM usuarios u JOIN empleados e ON e.empIdUsuario = u.usuId " +
                     "WHERE e.empId=? AND u.usuActivo=1";
        try (Connection cn = Conexion.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, empId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Usuario u = new Usuario();
                u.setUsuId(rs.getInt("usuId"));
                u.setUsuUsername(rs.getString("usuUsername"));
                u.setUsuPassword(rs.getString("usuPassword"));
                u.setUsuPerfil(Usuario.Perfil.valueOf(rs.getString("usuPerfil")));
                u.setUsuActivo(rs.getBoolean("usuActivo"));
                return u;
            }
        }
        return null;
    }
}
