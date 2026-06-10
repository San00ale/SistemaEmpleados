package sistema.view;

import sistema.dao.UsuarioDAO;
import sistema.model.Usuario;
import sistema.util.Estilos;
import sistema.util.Sesion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Pantalla de inicio de sesión — UTA
 *
 * Dos pestañas:
 *   TAB 1 → Credenciales (admin y empleados con cuenta)
 *   TAB 2 → Reconocimiento Facial (webcam)
 *
 * Al iniciar sesión como EMPLEADO, el registro de asistencia
 * se realiza desde la ventana principal con los botones
 * "Registrar Entrada" y "Registrar Salida".
 */
public class FrmLogin extends JFrame {

    private JTextField      txtUsuario;
    private JPasswordField  txtPassword;
    private JLabel          lblMensaje;
    private PnlLoginFacial  pnlFacial;
    private JTabbedPane     tabs;
    private boolean         ventanaPrincipalAbierta = false;

    public FrmLogin() {
        initComponents();
    }

    // ═══════════════════════════════════════════════════════════
    // UI
    // ═══════════════════════════════════════════════════════════
    private void initComponents() {
        setTitle("Sistema de Empleados — UTA | Inicio de Sesión");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(460, 590);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout());

        // ── Cabecera UTA ──
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(Estilos.ROJO_UTA);
        pnlHeader.setPreferredSize(new Dimension(460, 110));
        pnlHeader.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel lblUTA = new JLabel("UTA", SwingConstants.CENTER);
        lblUTA.setFont(new Font("Segoe UI", Font.BOLD, 50));
        lblUTA.setForeground(Estilos.BLANCO);

        JLabel lblInst = new JLabel(
            "<html><center>Universidad Técnica de Ambato<br>" +
            "<span style='font-size:9pt;'>Sistema de Registro de Empleados</span>" +
            "</center></html>", SwingConstants.CENTER);
        lblInst.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblInst.setForeground(Estilos.BLANCO);

        JPanel pnlTxt = new JPanel(new GridLayout(2, 1, 0, 2));
        pnlTxt.setOpaque(false);
        pnlTxt.add(lblUTA);
        pnlTxt.add(lblInst);
        pnlHeader.add(pnlTxt, BorderLayout.CENTER);

        // ── Tabs ──
        tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabs.addTab("🔑  Credenciales", buildTabCredenciales());
        tabs.addTab("📷  Facial",        buildTabFacial());

        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 1 && pnlFacial != null)
                pnlFacial.iniciarCamara();
            else if (pnlFacial != null)
                pnlFacial.detenerCamara();
        });

        // ── Pie ──
        JLabel lblPie = new JLabel("Sistema de Asistencia v1.0  —  UTA © 2026",
                SwingConstants.CENTER);
        lblPie.setFont(Estilos.FUENTE_PEQUENA);
        lblPie.setForeground(Estilos.GRIS_BORDE);
        lblPie.setBorder(BorderFactory.createEmptyBorder(4, 0, 6, 0));

        add(pnlHeader, BorderLayout.NORTH);
        add(tabs,      BorderLayout.CENTER);
        add(lblPie,    BorderLayout.SOUTH);
    }

    // ── TAB 1: Credenciales ───────────────────────────────────
    private JPanel buildTabCredenciales() {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setBackground(Estilos.BLANCO);
        pnl.setBorder(BorderFactory.createEmptyBorder(20, 40, 16, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(7, 0, 7, 0);
        gbc.gridx = 0; gbc.weightx = 1;

        // Título
        JLabel lblTit = new JLabel("Iniciar Sesión", SwingConstants.CENTER);
        lblTit.setFont(Estilos.FUENTE_SUBTIT);
        lblTit.setForeground(Estilos.NEGRO_TEXTO);
        gbc.gridy = 0; pnl.add(lblTit, gbc);

        // Usuario
        gbc.gridy = 1; pnl.add(etq("Usuario:"), gbc);
        txtUsuario = new JTextField(20);
        Estilos.estilizarCampo(txtUsuario);
        gbc.gridy = 2; pnl.add(txtUsuario, gbc);

        // Contraseña
        gbc.gridy = 3; pnl.add(etq("Contraseña:"), gbc);
        txtPassword = new JPasswordField(20);
        Estilos.estilizarCampo(txtPassword);
        gbc.gridy = 4; pnl.add(txtPassword, gbc);

        // Botón ingresar al sistema
        JButton btnIngresar = new JButton("INGRESAR AL SISTEMA");
        Estilos.estilizarBotonPrimario(btnIngresar);
        btnIngresar.setPreferredSize(new Dimension(220, 40));
        gbc.gridy = 5; gbc.insets = new Insets(18, 30, 4, 30);
        pnl.add(btnIngresar, gbc);

        // ── Separador ──
        gbc.gridy = 6; gbc.insets = new Insets(10, 0, 6, 0);
        JSeparator sep = new JSeparator();
        sep.setForeground(Estilos.GRIS_BORDE);
        pnl.add(sep, gbc);

        // Mensaje de error/estado
        lblMensaje = new JLabel(" ", SwingConstants.CENTER);
        lblMensaje.setFont(Estilos.FUENTE_PEQUENA);
        lblMensaje.setForeground(Estilos.ROJO_ERROR);
        gbc.gridy = 7; gbc.insets = new Insets(2, 0, 0, 0);
        pnl.add(lblMensaje, gbc);

        // ── Eventos ──
        btnIngresar.addActionListener(e -> autenticarYAbrir());
        txtPassword.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) autenticarYAbrir();
            }
        });
        txtUsuario.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) txtPassword.requestFocus();
            }
        });

        return pnl;
    }

    // ── TAB 2: Facial ─────────────────────────────────────────
    private JPanel buildTabFacial() {
        JPanel pnl = new JPanel(new BorderLayout(0, 6));
        pnl.setBackground(Estilos.BLANCO);
        pnl.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JLabel lblTit = new JLabel(
            "<html><center>Iniciar sesión por reconocimiento facial<br>" +
            "<font size='2' color='gray'>Requiere foto registrada por el administrador</font>" +
            "</center></html>", SwingConstants.CENTER);
        lblTit.setFont(Estilos.FUENTE_SUBTIT);
        lblTit.setForeground(Estilos.NEGRO_TEXTO);

        pnlFacial = new PnlLoginFacial(this::abrirPrincipal);
        pnl.add(lblTit,    BorderLayout.NORTH);
        pnl.add(pnlFacial, BorderLayout.CENTER);
        return pnl;
    }

    // ═══════════════════════════════════════════════════════════
    // AUTENTICACIÓN → ABRIR SISTEMA
    // ═══════════════════════════════════════════════════════════
    private void autenticarYAbrir() {
        String user = txtUsuario.getText().trim();
        String pass = new String(txtPassword.getPassword());
        if (user.isEmpty() || pass.isEmpty()) {
            mostrarError("Ingrese usuario y contraseña."); return;
        }
        try {
            Usuario u = new UsuarioDAO().autenticar(user, pass);
            if (u != null) {
                Sesion.getInstance().iniciar(u);
                abrirPrincipal();
            } else {
                mostrarError("Credenciales incorrectas.");
                txtPassword.setText("");
            }
        } catch (Exception ex) {
            mostrarError("Error de conexión con la BD.");
            ex.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════
    private void abrirPrincipal() {
        SwingUtilities.invokeLater(() -> {
            if (!ventanaPrincipalAbierta) {
                ventanaPrincipalAbierta = true;
                new FrmPrincipal().setVisible(true);
                dispose();
            }
        });
    }

    private void mostrarError(String msg) {
        lblMensaje.setText(msg);
        lblMensaje.setForeground(Estilos.ROJO_ERROR);
    }

    private JLabel etq(String t) {
        return Estilos.etiquetaFormulario(t);
    }

    // ── Main ──────────────────────────────────────────────────
    public static void main(String[] args) {
        Estilos.aplicarTemaEntradaTextoNegro();
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        Estilos.aplicarTemaEntradaTextoNegro();
        SwingUtilities.invokeLater(() -> new FrmLogin().setVisible(true));
    }
}
