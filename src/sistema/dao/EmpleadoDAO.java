package sistema.dao;

import sistema.model.Empleado;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la tabla empleados — prefijo emp.
 * La cédula empCedula es CHAR(10) solo numérica; validación también en Java.
 */
public class EmpleadoDAO {

    private static final String SELECT_BASE =
        "SELECT empId, empCedula, empNombres, empApellidos, empCorreo, empTelefono, " +
        "       empCargo, empTipoJornada, empFechaIngreso, empActivo, " +
        "       empIdUsuario, empTieneFoto " +
        "FROM empleados ";

    // ── Mapeo ResultSet → Empleado ────────────────────────────
    private Empleado mapear(ResultSet rs) throws SQLException {
        Empleado e = new Empleado();
        e.setEmpId(rs.getInt("empId"));
        e.setEmpCedula(rs.getString("empCedula"));
        e.setEmpNombres(rs.getString("empNombres"));
        e.setEmpApellidos(rs.getString("empApellidos"));
        e.setEmpCorreo(rs.getString("empCorreo"));
        e.setEmpTelefono(rs.getString("empTelefono"));
        e.setEmpCargo(rs.getString("empCargo"));
        e.setEmpTipoJornada(Empleado.TipoJornada.valueOf(rs.getString("empTipoJornada")));
        Date fi = rs.getDate("empFechaIngreso");
        e.setEmpFechaIngreso(fi != null ? fi.toLocalDate() : null);
        e.setEmpActivo(rs.getBoolean("empActivo"));
        int idu = rs.getInt("empIdUsuario");
        e.setEmpIdUsuario(rs.wasNull() ? null : idu);
        e.setEmpTieneFoto(rs.getBoolean("empTieneFoto"));
        return e;
    }

    // ── CRUD ─────────────────────────────────────────────────

    /** Todos los empleados activos. */
    public List<Empleado> listar() throws Exception {
        List<Empleado> lista = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE empActivo = 1 ORDER BY empApellidos, empNombres";
        try (Connection cn = Conexion.getConexion();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    /** Todos (activos e inactivos) — solo ADMIN. */
    public List<Empleado> listarTodos() throws Exception {
        List<Empleado> lista = new ArrayList<>();
        String sql = SELECT_BASE + "ORDER BY empActivo DESC, empApellidos, empNombres";
        try (Connection cn = Conexion.getConexion();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    /** Buscar por cédula. */
    public Empleado buscarPorCedula(String cedula) throws Exception {
        String sql = SELECT_BASE + "WHERE empCedula = ?";
        try (Connection cn = Conexion.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, cedula);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapear(rs) : null;
        }
    }

    /** Buscar por empId. */
    public Empleado buscarPorId(int id) throws Exception {
        String sql = SELECT_BASE + "WHERE empId = ?";
        try (Connection cn = Conexion.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapear(rs) : null;
        }
    }

    /** Buscar por empIdUsuario (para login facial). */
    public Empleado buscarPorIdUsuario(int idUsuario) throws Exception {
        String sql = SELECT_BASE + "WHERE empIdUsuario = ? AND empActivo = 1";
        try (Connection cn = Conexion.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapear(rs) : null;
        }
    }

    /** Insertar; retorna empId generado. */
    public int insertar(Empleado e) throws Exception {
        String sql = "INSERT INTO empleados(empCedula, empNombres, empApellidos, empCorreo, " +
                     "empTelefono, empCargo, empTipoJornada, empFechaIngreso, empActivo, empIdUsuario) " +
                     "VALUES(?,?,?,?,?,?,?,?,1,?)";
        try (Connection cn = Conexion.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.getEmpCedula());
            ps.setString(2, e.getEmpNombres());
            ps.setString(3, e.getEmpApellidos());
            ps.setString(4, e.getEmpCorreo());
            ps.setString(5, e.getEmpTelefono());
            ps.setString(6, e.getEmpCargo());
            ps.setString(7, e.getEmpTipoJornada().name());
            ps.setDate(8, e.getEmpFechaIngreso() != null
                    ? Date.valueOf(e.getEmpFechaIngreso()) : Date.valueOf(LocalDate.now()));
            if (e.getEmpIdUsuario() != null) ps.setInt(9, e.getEmpIdUsuario());
            else ps.setNull(9, Types.INTEGER);
            ps.executeUpdate();
            ResultSet rk = ps.getGeneratedKeys();
            return rk.next() ? rk.getInt(1) : -1;
        }
    }

    /** Actualizar datos. */
    public boolean actualizar(Empleado e) throws Exception {
        String sql = "UPDATE empleados SET empCedula=?, empNombres=?, empApellidos=?, " +
                     "empCorreo=?, empTelefono=?, empCargo=?, empTipoJornada=?, " +
                     "empFechaIngreso=?, empIdUsuario=? WHERE empId=?";
        try (Connection cn = Conexion.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, e.getEmpCedula());
            ps.setString(2, e.getEmpNombres());
            ps.setString(3, e.getEmpApellidos());
            ps.setString(4, e.getEmpCorreo());
            ps.setString(5, e.getEmpTelefono());
            ps.setString(6, e.getEmpCargo());
            ps.setString(7, e.getEmpTipoJornada().name());
            ps.setDate(8, e.getEmpFechaIngreso() != null
                    ? Date.valueOf(e.getEmpFechaIngreso()) : null);
            if (e.getEmpIdUsuario() != null) ps.setInt(9, e.getEmpIdUsuario());
            else ps.setNull(9, Types.INTEGER);
            ps.setInt(10, e.getEmpId());
            return ps.executeUpdate() > 0;
        }
    }

    /** Baja lógica. */
    public boolean eliminar(int empId) throws Exception {
        String sql = "UPDATE empleados SET empActivo = 0 WHERE empId = ?";
        try (Connection cn = Conexion.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, empId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Verifica cédula duplicada. */
    public boolean existeCedula(String cedula) throws Exception {
        String sql = "SELECT COUNT(*) FROM empleados WHERE empCedula = ?";
        try (Connection cn = Conexion.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, cedula);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }
}
