package sistema.util;

import sistema.model.Usuario;

/**
 * Singleton de sesión activa durante la ejecución del sistema.
 */
public final class Sesion {

    private static Sesion instancia;
    private Usuario usuarioActual;

    private Sesion() {}

    public static Sesion getInstance() {
        if (instancia == null) instancia = new Sesion();
        return instancia;
    }

    public Usuario getUsuarioActual()  { return usuarioActual; }
    public void    iniciar(Usuario u)  { this.usuarioActual = u; }
    public void    cerrar()            { this.usuarioActual = null; }

    public boolean isAdmin() {
        return usuarioActual != null && usuarioActual.isAdmin();
    }
    public boolean isLogueado() { return usuarioActual != null; }
}
