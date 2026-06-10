package sistema.model;

/**
 * Entidad Usuario — campos con prefijo usu según convención del ING.
 */
public class Usuario {

    public enum Perfil { ADMIN, EMPLEADO }

    private int    usuId;
    private String usuUsername;
    private String usuPassword;   // SHA-256 hex
    private Perfil usuPerfil;
    private boolean usuActivo;

    public Usuario() {}

    public Usuario(int id, String username, String password, Perfil perfil, boolean activo) {
        this.usuId       = id;
        this.usuUsername = username;
        this.usuPassword = password;
        this.usuPerfil   = perfil;
        this.usuActivo   = activo;
    }

    public int     getUsuId()          { return usuId; }
    public void    setUsuId(int v)     { usuId = v; }

    public String  getUsuUsername()    { return usuUsername; }
    public void    setUsuUsername(String v){ usuUsername = v; }

    public String  getUsuPassword()    { return usuPassword; }
    public void    setUsuPassword(String v){ usuPassword = v; }

    public Perfil  getUsuPerfil()      { return usuPerfil; }
    public void    setUsuPerfil(Perfil v){ usuPerfil = v; }

    public boolean isUsuActivo()       { return usuActivo; }
    public void    setUsuActivo(boolean v){ usuActivo = v; }

    public boolean isAdmin()           { return Perfil.ADMIN.equals(usuPerfil); }

    @Override
    public String toString() { return usuUsername + " [" + usuPerfil + "]"; }
}
