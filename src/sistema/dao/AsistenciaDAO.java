package sistema.dao;

import sistema.model.Asistencia;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la tabla asistencias — prefijo asi.
 *
 * Lógica del botón "Registrar Asistencia":
 *   - Se llama a registrarMarca(empId) con la hora actual.
 *   - Detecta automáticamente qué campo rellenar:
 *       asiEntradaManana → asiSalidaManana → asiEntradaTarde → asiSalidaTarde
 *   - Calcula atrasos y salidas anticipadas en el momento del registro.
 */
public class AsistenciaDAO {

    // ── Mapeo ─────────────────────────────────────────────────
    private Asistencia mapear(ResultSet rs) throws SQLException {
        Asistencia a = new Asistencia();
        a.setAsiId(rs.getInt("asiId"));
        a.setAsiIdEmpleado(rs.getInt("asiIdEmpleado"));
        a.setAsiFecha(rs.getDate("asiFecha").toLocalDate());
        a.setAsiEntradaManana(toLocalTime(rs.getTime("asiEntradaManana")));
        a.setAsiSalidaManana(toLocalTime(rs.getTime("asiSalidaManana")));
        a.setAsiEntradaTarde(toLocalTime(rs.getTime("asiEntradaTarde")));
        a.setAsiSalidaTarde(toLocalTime(rs.getTime("asiSalidaTarde")));
        a.setAsiAtrasoMinutos(rs.getInt("asiAtrasoMinutos"));
        a.setAsiSalidaAntMinutos(rs.getInt("asiSalidaAntMinutos"));
        a.setAsiTotalDescMinutos(rs.getInt("asiTotalDescMinutos"));
        a.setAsiHorasTrabajadas(rs.getBigDecimal("asiHorasTrabajadas"));
        a.setAsiObservacion(rs.getString("asiObservacion"));
        return a;
    }

    private LocalTime toLocalTime(Time t) { return t != null ? t.toLocalTime() : null; }
    private void setTime(PreparedStatement ps, int i, LocalTime t) throws SQLException {
        if (t != null) ps.setTime(i, Time.valueOf(t));
        else           ps.setNull(i, Types.TIME);
    }

    // ── Consultas ─────────────────────────────────────────────

    /** Obtiene el registro del día para un empleado; null si no existe. */
    public Asistencia obtenerDelDia(int empId, LocalDate fecha) throws Exception {
        String sql = "SELECT * FROM asistencias WHERE asiIdEmpleado=? AND asiFecha=?";
        try (Connection cn = Conexion.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, empId);
            ps.setDate(2, Date.valueOf(fecha));
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapear(rs) : null;
        }
    }

    /** Reporte mensual de un empleado. */
    public List<Asistencia> reporteMensual(int empId, int anio, int mes) throws Exception {
        String sql = "SELECT a.*, e.empCedula, " +
                     "CONCAT(e.empNombres,' ',e.empApellidos) AS nombre_emp, e.empTipoJornada " +
                     "FROM asistencias a JOIN empleados e ON e.empId = a.asiIdEmpleado " +
                     "WHERE a.asiIdEmpleado=? AND YEAR(a.asiFecha)=? AND MONTH(a.asiFecha)=? " +
                     "ORDER BY a.asiFecha";
        List<Asistencia> lista = new ArrayList<>();
        try (Connection cn = Conexion.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, empId);
            ps.setInt(2, anio);
            ps.setInt(3, mes);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Asistencia a = mapear(rs);
                a.setCedulaEmpleado(rs.getString("empCedula"));
                a.setNombreEmpleado(rs.getString("nombre_emp"));
                a.setTipoJornada(rs.getString("empTipoJornada"));
                lista.add(a);
            }
        }
        return lista;
    }

    /** Asistencias de todos los empleados en una fecha. */
    public List<Asistencia> porFecha(LocalDate fecha) throws Exception {
        String sql = "SELECT a.*, e.empCedula, " +
                     "CONCAT(e.empNombres,' ',e.empApellidos) AS nombre_emp, e.empTipoJornada " +
                     "FROM asistencias a JOIN empleados e ON e.empId = a.asiIdEmpleado " +
                     "WHERE a.asiFecha=? ORDER BY e.empApellidos, e.empNombres";
        List<Asistencia> lista = new ArrayList<>();
        try (Connection cn = Conexion.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(fecha));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Asistencia a = mapear(rs);
                a.setCedulaEmpleado(rs.getString("empCedula"));
                a.setNombreEmpleado(rs.getString("nombre_emp"));
                a.setTipoJornada(rs.getString("empTipoJornada"));
                lista.add(a);
            }
        }
        return lista;
    }

    // ── Lógica del botón "Registrar Asistencia" ──────────────

    /**
     * Registra la marca de tiempo actual para el empleado.
     * Detecta automáticamente qué campo rellenar según el estado del día.
     *
     * Retorna un mensaje descriptivo de lo que se registró.
     * Lanza excepción si el día ya está completo.
     */
    public String registrarMarca(int empId) throws Exception {
        LocalDate hoy   = LocalDate.now();
        LocalTime ahora = LocalTime.now();

        Asistencia a = obtenerDelDia(empId, hoy);

        if (a == null) {
            // Primera marca del día → crear registro con entrada mañana
            a = new Asistencia();
            a.setAsiIdEmpleado(empId);
            a.setAsiFecha(hoy);
            a.setAsiEntradaManana(ahora);

            // Calcular atraso si llega después de las 08:00
            int atrasoM = (int) Math.max(0,
                java.time.Duration.between(Asistencia.HORA_ENTRADA_MANANA, ahora).toMinutes());
            a.setAsiAtrasoMinutos(atrasoM);
            a.setAsiTotalDescMinutos(atrasoM);
            a.setAsiHorasTrabajadas(BigDecimal.ZERO);
            insertarNuevo(a);
            return String.format("✅ ENTRADA MAÑANA registrada: %s%s",
                ahora.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                atrasoM > 0 ? " ⚠ Atraso: " + atrasoM + " min ($" +
                    String.format("%.2f", atrasoM * 0.20) + ")" : " — puntual");
        }

        // Segunda marca → salida mañana (si aún no la tiene y es antes de las 14:00)
        if (a.getAsiSalidaManana() == null && ahora.isBefore(LocalTime.of(14, 0))) {
            a.setAsiSalidaManana(ahora);
            // Salida anticipada mañana
            int salidaAntM = (int) Math.max(0,
                java.time.Duration.between(ahora, Asistencia.HORA_SALIDA_MANANA).toMinutes());
            a.setAsiSalidaAntMinutos(a.getAsiSalidaAntMinutos() + salidaAntM);
            a.setAsiTotalDescMinutos(a.getAsiAtrasoMinutos() + a.getAsiSalidaAntMinutos());
            recalcularHoras(a);
            actualizarCompleto(a);
            return String.format("✅ SALIDA MAÑANA registrada: %s%s",
                ahora.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                salidaAntM > 0 ? " ⚠ Salida anticipada: " + salidaAntM + " min ($" +
                    String.format("%.2f", salidaAntM * 0.20) + ")" : "");
        }

        // Tercera marca → entrada tarde
        if (a.getAsiEntradaTarde() == null) {
            if (ahora.isBefore(LocalTime.of(13, 59))) {
                throw new Exception(
                    "⚠ Aún es horario de mañana o almuerzo.\n" +
                    "La entrada tarde se puede registrar a partir de las 14:00.");
            }
            a.setAsiEntradaTarde(ahora);
            // Atraso entrada tarde
            int atrasoT = (int) Math.max(0,
                java.time.Duration.between(Asistencia.HORA_ENTRADA_TARDE, ahora).toMinutes());
            a.setAsiAtrasoMinutos(a.getAsiAtrasoMinutos() + atrasoT);
            a.setAsiTotalDescMinutos(a.getAsiAtrasoMinutos() + a.getAsiSalidaAntMinutos());
            recalcularHoras(a);
            actualizarCompleto(a);
            return String.format("✅ ENTRADA TARDE registrada: %s%s",
                ahora.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                atrasoT > 0 ? " ⚠ Atraso: " + atrasoT + " min ($" +
                    String.format("%.2f", atrasoT * 0.20) + ")" : " — puntual");
        }

        // Cuarta marca → salida tarde
        if (a.getAsiSalidaTarde() == null) {
            a.setAsiSalidaTarde(ahora);
            // Salida anticipada tarde
            int salidaAntT = (int) Math.max(0,
                java.time.Duration.between(ahora, Asistencia.HORA_SALIDA_TARDE).toMinutes());
            a.setAsiSalidaAntMinutos(a.getAsiSalidaAntMinutos() + salidaAntT);
            a.setAsiTotalDescMinutos(a.getAsiAtrasoMinutos() + a.getAsiSalidaAntMinutos());
            recalcularHoras(a);
            actualizarCompleto(a);
            return String.format("✅ SALIDA TARDE registrada: %s%s\n" +
                "🕐 Total horas trabajadas hoy: %s",
                ahora.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                salidaAntT > 0 ? " ⚠ Salida anticipada: " + salidaAntT + " min ($" +
                    String.format("%.2f", salidaAntT * 0.20) + ")" : "",
                a.getAsiHorasTrabajadas().toPlainString());
        }

        // Ya completó las 4 marcas
        throw new Exception("ℹ Ya registró todas las marcas del día.\n" +
            "Entrada mañana: " + fmt(a.getAsiEntradaManana()) +
            " | Salida mañana: " + fmt(a.getAsiSalidaManana()) + "\n" +
            "Entrada tarde: " + fmt(a.getAsiEntradaTarde()) +
            " | Salida tarde: " + fmt(a.getAsiSalidaTarde()));
    }

    /** Recalcula horas trabajadas efectivas en el objeto Asistencia. */
    private void recalcularHoras(Asistencia a) {
        double mins = 0;
        if (a.getAsiEntradaManana() != null && a.getAsiSalidaManana() != null) {
            mins += java.time.Duration.between(
                a.getAsiEntradaManana(), a.getAsiSalidaManana()).toMinutes();
        }
        if (a.getAsiEntradaTarde() != null && a.getAsiSalidaTarde() != null) {
            mins += java.time.Duration.between(
                a.getAsiEntradaTarde(), a.getAsiSalidaTarde()).toMinutes();
        }
        double horas = Math.max(0, mins / 60.0);
        a.setAsiHorasTrabajadas(
            java.math.BigDecimal.valueOf(horas)
                .setScale(2, java.math.RoundingMode.HALF_UP));
    }

    private String fmt(LocalTime t) {
        return t != null ? t.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) : "--";
    }

    // ── CRUD interno ──────────────────────────────────────────

    private void insertarNuevo(Asistencia a) throws Exception {
        String sql = "INSERT INTO asistencias(asiIdEmpleado, asiFecha, asiEntradaManana, " +
                     "asiAtrasoMinutos, asiSalidaAntMinutos, asiTotalDescMinutos, " +
                     "asiHorasTrabajadas) VALUES(?,?,?,?,?,?,?)";
        try (Connection cn = Conexion.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getAsiIdEmpleado());
            ps.setDate(2, Date.valueOf(a.getAsiFecha()));
            setTime(ps, 3, a.getAsiEntradaManana());
            ps.setInt(4, a.getAsiAtrasoMinutos());
            ps.setInt(5, a.getAsiSalidaAntMinutos());
            ps.setInt(6, a.getAsiTotalDescMinutos());
            ps.setBigDecimal(7, a.getAsiHorasTrabajadas());
            ps.executeUpdate();
            ResultSet rk = ps.getGeneratedKeys();
            if (rk.next()) a.setAsiId(rk.getInt(1));
        }
    }

    private void actualizarCompleto(Asistencia a) throws Exception {
        String sql = "UPDATE asistencias SET " +
                     "asiEntradaManana=?, asiSalidaManana=?, " +
                     "asiEntradaTarde=?, asiSalidaTarde=?, " +
                     "asiAtrasoMinutos=?, asiSalidaAntMinutos=?, asiTotalDescMinutos=?, " +
                     "asiHorasTrabajadas=? WHERE asiId=?";
        try (Connection cn = Conexion.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            setTime(ps, 1, a.getAsiEntradaManana());
            setTime(ps, 2, a.getAsiSalidaManana());
            setTime(ps, 3, a.getAsiEntradaTarde());
            setTime(ps, 4, a.getAsiSalidaTarde());
            ps.setInt(5, a.getAsiAtrasoMinutos());
            ps.setInt(6, a.getAsiSalidaAntMinutos());
            ps.setInt(7, a.getAsiTotalDescMinutos());
            ps.setBigDecimal(8, a.getAsiHorasTrabajadas());
            ps.setInt(9, a.getAsiId());
            ps.executeUpdate();
        }
    }
}
