package sistema.dao;

import sistema.model.Usuario;
import sistema.util.Utilidades;

import java.sql.*;

/**
 * DAO para la tabla usuarios — prefijo usu.
 */
public class UsuarioDAO {

    /** Autentica: retorna Usuario o null si falla. */
    public Usuario autenticar(String username, String password) throws Exception {
        String sql = "SELECT usuId, usuUsername, usuPassword, usuPerfil, usuActivo " +
                     "FROM usuarios WHERE usuUsername = ? AND usuActivo = 1";
        try (Connection cn = Conexion.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String hashBD = rs.getString("usuPassword");
                if (hashBD.equalsIgnoreCase(Utilidades.sha256(password))) {
                    Usuario u = new Usuario();
                    u.setUsuId(rs.getInt("usuId"));
                    u.setUsuUsername(rs.getString("usuUsername"));
                    u.setUsuPassword(hashBD);
                    u.setUsuPerfil(Usuario.Perfil.valueOf(rs.getString("usuPerfil")));
                    u.setUsuActivo(rs.getBoolean("usuActivo"));
                    return u;
                }
            }
        }
        return null;
    }

    /** Inserta usuario; retorna id generado. */
    public int insertar(Usuario u) throws Exception {
        String sql = "INSERT INTO usuarios(usuUsername, usuPassword, usuPerfil, usuActivo) VALUES(?,?,?,1)";
        try (Connection cn = Conexion.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getUsuUsername());
            ps.setString(2, u.getUsuPassword());
            ps.setString(3, u.getUsuPerfil().name());
            ps.executeUpdate();
            ResultSet rk = ps.getGeneratedKeys();
            return rk.next() ? rk.getInt(1) : -1;
        }
    }

    /** Verifica si ya existe un username. */
    public boolean existeUsername(String username) throws Exception {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE usuUsername = ?";
        try (Connection cn = Conexion.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    /** Cambia contraseña. */
    public boolean cambiarPassword(int id, String nuevaPass) throws Exception {
        String sql = "UPDATE usuarios SET usuPassword = ? WHERE usuId = ?";
        try (Connection cn = Conexion.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, Utilidades.sha256(nuevaPass));
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }
}
