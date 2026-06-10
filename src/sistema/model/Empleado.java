package sistema.model;

import java.time.LocalDate;

/**
 * Entidad Empleado — campos con prefijo emp según convención del ING.
 * empTipoJornada: COMPLETO (sueldo fijo $1500/mes) | PARCIAL ($5/hora, máx 8h/día)
 */
public class Empleado {

    public enum TipoJornada { COMPLETO, PARCIAL }

    private int          empId;
    private String       empCedula;       // CHAR(10), solo dígitos
    private String       empNombres;
    private String       empApellidos;
    private String       empCorreo;
    private String       empTelefono;
    private String       empCargo;
    private TipoJornada  empTipoJornada;
    private LocalDate    empFechaIngreso;
    private boolean      empActivo;
    private Integer      empIdUsuario;    // null si no tiene cuenta
    private boolean      empTieneFoto;
    private byte[]       empFotoRostro;

    public Empleado() {}

    // ── Getters / Setters ─────────────────────────────────────
    public int          getEmpId()            { return empId; }
    public void         setEmpId(int v)       { empId = v; }

    public String       getEmpCedula()        { return empCedula; }
    public void         setEmpCedula(String v){ empCedula = v; }

    public String       getEmpNombres()       { return empNombres; }
    public void         setEmpNombres(String v){ empNombres = v; }

    public String       getEmpApellidos()     { return empApellidos; }
    public void         setEmpApellidos(String v){ empApellidos = v; }

    public String       getEmpCorreo()        { return empCorreo; }
    public void         setEmpCorreo(String v){ empCorreo = v; }

    public String       getEmpTelefono()      { return empTelefono; }
    public void         setEmpTelefono(String v){ empTelefono = v; }

    public String       getEmpCargo()         { return empCargo; }
    public void         setEmpCargo(String v) { empCargo = v; }

    public TipoJornada  getEmpTipoJornada()   { return empTipoJornada; }
    public void         setEmpTipoJornada(TipoJornada v){ empTipoJornada = v; }

    public LocalDate    getEmpFechaIngreso()  { return empFechaIngreso; }
    public void         setEmpFechaIngreso(LocalDate v){ empFechaIngreso = v; }

    public boolean      isEmpActivo()         { return empActivo; }
    public void         setEmpActivo(boolean v){ empActivo = v; }

    public Integer      getEmpIdUsuario()     { return empIdUsuario; }
    public void         setEmpIdUsuario(Integer v){ empIdUsuario = v; }

    public boolean      isEmpTieneFoto()      { return empTieneFoto; }
    public void         setEmpTieneFoto(boolean v){ empTieneFoto = v; }

    public byte[]       getEmpFotoRostro()    { return empFotoRostro; }
    public void         setEmpFotoRostro(byte[] v){ empFotoRostro = v; }

    public String       getNombreCompleto()   { return empNombres + " " + empApellidos; }

    @Override
    public String toString() { return empCedula + " - " + getNombreCompleto(); }
}
