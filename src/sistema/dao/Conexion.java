package sistema.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton de conexión a MySQL.
 * Local (XAMPP): valores por defecto abajo.
 * Docker: variables de entorno DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD.
 */
public class Conexion {

    private static final String DB_HOST = env("DB_HOST", "localhost");
    private static final String DB_PORT = env("DB_PORT", "3306");
    private static final String DB_NAME = env("DB_NAME", "bd_empleados");
    private static final String USER    = env("DB_USER", "root");
    private static final String PASS    = env("DB_PASSWORD", "");

    private static final String URL =
        "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME +
        "?useSSL=false&serverTimezone=America/Guayaquil&allowPublicKeyRetrieval=true";

    private static String env(String key, String porDefecto) {
        String v = System.getenv(key);
        return (v != null && !v.isBlank()) ? v.trim() : porDefecto;
    }

    private static Conexion instancia;
    private Connection connection;

    private Conexion() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                "No se encontró el driver MySQL (mysql-connector-j). " +
                "Agregue mysql-connector-j-9.x.jar en lib/.", e);
        }
        try {
            this.connection = DriverManager.getConnection(URL, USER, PASS);
        } catch (SQLException e) {
            throw mensajeConexionAmigable(e);
        }
    }

    public static Connection getConexion() throws SQLException {
        if (instancia == null || instancia.necesitaReconectar()) {
            cerrar();
            instancia = new Conexion();
        }
        return instancia.connection;
    }

    private boolean necesitaReconectar() throws SQLException {
        return connection == null || connection.isClosed() || !connection.isValid(2);
    }

    private static SQLException mensajeConexionAmigable(SQLException e) {
        String msg = e.getMessage();
        if (msg != null && (msg.contains("Communications link failure")
                || msg.contains("Connection refused")
                || msg.contains("connect timed out"))) {
            return new SQLException(
                "No se puede conectar a MySQL (" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME + ").\n" +
                "• Local: inicie MySQL en XAMPP y ejecute sql/bd_empleados.sql.\n" +
                "• Docker: docker compose up -d mysql\n" +
                "• Revise DB_HOST, DB_PORT, DB_USER y DB_PASSWORD.",
                e);
        }
        if (msg != null && msg.contains("Unknown database")) {
            return new SQLException(
                "La base de datos 'bd_empleados' no existe. Ejecute sql/bd_empleados.sql en phpMyAdmin.",
                e);
        }
        return e;
    }

    public static void cerrar() {
        try {
            if (instancia != null && instancia.connection != null && !instancia.connection.isClosed())
                instancia.connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            instancia = null;
        }
    }
}
