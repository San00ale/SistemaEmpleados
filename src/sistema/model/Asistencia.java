package sistema.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Entidad Asistencia — campos con prefijo asi según convención del ING.
 *
 * Jornada Mañana : 08:00 – 13:00
 * Almuerzo fijo  : 13:00 – 14:00 (no se registra)
 * Jornada Tarde  : 14:00 – 17:00
 * Total diario   : 8 horas
 *
 * Descuento: $0.20 × minuto (atraso entrada O salida anticipada)
 * Parcial  : horas_trabajadas × $5.00 (máx 8h/día)
 *
 * Lógica del botón "Registrar Asistencia" (hora actual automática):
 *   Toque 1 — hora actual < 14:00 → graba asiEntradaManana
 *   Toque 2 — hora actual < 14:00 → graba asiSalidaManana
 *   Toque 3 — hora actual >= 14:00 → graba asiEntradaTarde
 *   Toque 4 — hora actual >= 14:00 → graba asiSalidaTarde
 */
public class Asistencia {

    // Horarios estándar
    public static final LocalTime HORA_ENTRADA_MANANA = LocalTime.of( 8, 0);
    public static final LocalTime HORA_SALIDA_MANANA  = LocalTime.of(13, 0);
    public static final LocalTime HORA_ENTRADA_TARDE  = LocalTime.of(14, 0);
    public static final LocalTime HORA_SALIDA_TARDE   = LocalTime.of(17, 0);

    public static final double SUELDO_COMPLETO      = 1500.00;
    public static final double VALOR_HORA_PARCIAL   =    5.00;
    public static final double DESCUENTO_X_MINUTO   =    0.20;  // $0.20 por minuto
    public static final double MAX_HORAS_PARCIAL    =    8.00;

    private int        asiId;
    private int        asiIdEmpleado;
    private LocalDate  asiFecha;
    private LocalTime  asiEntradaManana;
    private LocalTime  asiSalidaManana;
    private LocalTime  asiEntradaTarde;
    private LocalTime  asiSalidaTarde;
    private int        asiAtrasoMinutos;       // minutos de llegada tarde
    private int        asiSalidaAntMinutos;    // minutos de salida anticipada
    private int        asiTotalDescMinutos;    // atraso + salida anticipada
    private BigDecimal asiHorasTrabajadas;
    private String     asiObservacion;

    // Campos enriquecidos para reportes (no columnas BD)
    private String nombreEmpleado;
    private String cedulaEmpleado;
    private String tipoJornada;

    public Asistencia() {}

    // ── Getters / Setters ──────────────────────────────────────
    public int        getAsiId()               { return asiId; }
    public void       setAsiId(int v)          { asiId = v; }

    public int        getAsiIdEmpleado()       { return asiIdEmpleado; }
    public void       setAsiIdEmpleado(int v)  { asiIdEmpleado = v; }

    public LocalDate  getAsiFecha()            { return asiFecha; }
    public void       setAsiFecha(LocalDate v) { asiFecha = v; }

    public LocalTime  getAsiEntradaManana()    { return asiEntradaManana; }
    public void       setAsiEntradaManana(LocalTime v){ asiEntradaManana = v; }

    public LocalTime  getAsiSalidaManana()     { return asiSalidaManana; }
    public void       setAsiSalidaManana(LocalTime v){ asiSalidaManana = v; }

    public LocalTime  getAsiEntradaTarde()     { return asiEntradaTarde; }
    public void       setAsiEntradaTarde(LocalTime v){ asiEntradaTarde = v; }

    public LocalTime  getAsiSalidaTarde()      { return asiSalidaTarde; }
    public void       setAsiSalidaTarde(LocalTime v){ asiSalidaTarde = v; }

    public int        getAsiAtrasoMinutos()    { return asiAtrasoMinutos; }
    public void       setAsiAtrasoMinutos(int v){ asiAtrasoMinutos = v; }

    public int        getAsiSalidaAntMinutos() { return asiSalidaAntMinutos; }
    public void       setAsiSalidaAntMinutos(int v){ asiSalidaAntMinutos = v; }

    public int        getAsiTotalDescMinutos() { return asiTotalDescMinutos; }
    public void       setAsiTotalDescMinutos(int v){ asiTotalDescMinutos = v; }

    public BigDecimal getAsiHorasTrabajadas()  { return asiHorasTrabajadas; }
    public void       setAsiHorasTrabajadas(BigDecimal v){ asiHorasTrabajadas = v; }

    public String     getAsiObservacion()      { return asiObservacion; }
    public void       setAsiObservacion(String v){ asiObservacion = v; }

    public String     getNombreEmpleado()      { return nombreEmpleado; }
    public void       setNombreEmpleado(String v){ nombreEmpleado = v; }

    public String     getCedulaEmpleado()      { return cedulaEmpleado; }
    public void       setCedulaEmpleado(String v){ cedulaEmpleado = v; }

    public String     getTipoJornada()         { return tipoJornada; }
    public void       setTipoJornada(String v) { tipoJornada = v; }

    // ── Cálculos ───────────────────────────────────────────────

    /** Minutos de atraso en entrada mañana (0 si llegó a tiempo o antes). */
    public int calcularAtrasoManana() {
        if (asiEntradaManana == null) return 0;
        long diff = java.time.Duration.between(HORA_ENTRADA_MANANA, asiEntradaManana).toMinutes();
        return (int) Math.max(0, diff);
    }

    /** Minutos de atraso en entrada tarde. */
    public int calcularAtrasoTarde() {
        if (asiEntradaTarde == null) return 0;
        long diff = java.time.Duration.between(HORA_ENTRADA_TARDE, asiEntradaTarde).toMinutes();
        return (int) Math.max(0, diff);
    }

    /** Minutos de salida anticipada mañana (salió antes de 13:00). */
    public int calcularSalidaAntManana() {
        if (asiSalidaManana == null) return 0;
        long diff = java.time.Duration.between(asiSalidaManana, HORA_SALIDA_MANANA).toMinutes();
        return (int) Math.max(0, diff);
    }

    /** Minutos de salida anticipada tarde (salió antes de 17:00). */
    public int calcularSalidaAntTarde() {
        if (asiSalidaTarde == null) return 0;
        long diff = java.time.Duration.between(asiSalidaTarde, HORA_SALIDA_TARDE).toMinutes();
        return (int) Math.max(0, diff);
    }

    /** Total descuento en USD. */
    public double calcularTotalDescuento() {
        return asiTotalDescMinutos * DESCUENTO_X_MINUTO;
    }

    /** Sueldo neto mensual para empleado COMPLETO. */
    public double calcularSueldoNetoCompleto() {
        return SUELDO_COMPLETO - calcularTotalDescuento();
    }

    /** Pago del día para empleado PARCIAL. */
    public double calcularPagoParcial() {
        if (asiHorasTrabajadas == null) return 0;
        double h = Math.min(asiHorasTrabajadas.doubleValue(), MAX_HORAS_PARCIAL);
        return h * VALOR_HORA_PARCIAL;
    }

    /**
     * Determina qué acción debe hacer el botón "Registrar Asistencia"
     * basándose en los registros existentes y la hora actual.
     * Retorna una descripción de la siguiente acción.
     */
    public String siguienteAccion() {
        LocalTime ahora = LocalTime.now();
        if (asiEntradaManana == null)
            return "Registrar ENTRADA mañana";
        if (asiSalidaManana == null && ahora.isBefore(LocalTime.of(13, 30)))
            return "Registrar SALIDA mañana";
        if (asiEntradaTarde == null && ahora.isAfter(LocalTime.of(13, 45)))
            return "Registrar ENTRADA tarde";
        if (asiSalidaTarde == null)
            return "Registrar SALIDA tarde";
        return "COMPLETO — ya registró las 4 marcas del día";
    }
}
