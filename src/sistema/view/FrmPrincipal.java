package sistema.view;

import sistema.dao.AsistenciaDAO;
import sistema.dao.EmpleadoDAO;
import sistema.model.Empleado;
import sistema.model.Usuario;
import sistema.util.Estilos;
import sistema.util.Sesion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Ventana principal del sistema.
 *
 * ADMIN   → acceso completo a todo.
 * EMPLEADO → solo puede ver sus propias asistencias por fecha.
 *            (El registro de asistencia lo hace desde el LOGIN con el botón verde)
 */
public class FrmPrincipal extends JFrame {

    private JPanel pnlContenido;
    private JLabel lblReloj;

    public FrmPrincipal() {
        initComponents();
        iniciarReloj();
    }

    private void initComponents() {
        boolean esAdmin = Sesion.getInstance().isAdmin();
        String  usuario = Sesion.getInstance().getUsuarioActual().getUsuUsername();

        setTitle("Sistema de Empleados — UTA  |  " + usuario +
                 "  [" + (esAdmin ? "ADMINISTRADOR" : "EMPLEADO") + "]");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 680);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ── Cabecera roja UTA ──────────────────────────────────
        JPanel pnlTop = new JPanel(new BorderLayout());
        pnlTop.setBackground(Estilos.ROJO_UTA);
        pnlTop.setPreferredSize(new Dimension(0, 55));
        pnlTop.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        JLabel lblTitulo = new JLabel(
            "🎓  Universidad Técnica de Ambato — Sistema de Empleados");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitulo.setForeground(Estilos.BLANCO);

        lblReloj = new JLabel("", SwingConstants.RIGHT);
        lblReloj.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblReloj.setForeground(new Color(255, 220, 220));

        pnlTop.add(lblTitulo, BorderLayout.WEST);
        pnlTop.add(lblReloj,  BorderLayout.EAST);

        // ── Menú lateral ───────────────────────────────────────
        JPanel pnlMenu = new JPanel();
        pnlMenu.setLayout(new BoxLayout(pnlMenu, BoxLayout.Y_AXIS));
        pnlMenu.setBackground(Estilos.ROJO_OSCURO);
        pnlMenu.setPreferredSize(new Dimension(215, 0));
        pnlMenu.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        // ── Sección CONSULTAS (ambos perfiles) ──
        pnlMenu.add(seccion("CONSULTAS"));
        pnlMenu.add(botonMenu("📋  Mis Asistencias",
                () -> abrir(new FrmRegistroAsistencia())));
        pnlMenu.add(botonMenu("📅  Asistencia por Fecha",
                () -> abrir(new FrmReporteFecha())));
        pnlMenu.add(Box.createVerticalStrut(4));

        // ── Sección REGISTRO DE ASISTENCIA (solo empleados) ──
        if (!esAdmin) {
            pnlMenu.add(seccion("REGISTRO ASISTENCIA"));
            pnlMenu.add(botonMenu("🟢  Registrar Entrada", this::registrarEntrada));
            pnlMenu.add(botonMenu("🔴  Registrar Salida", this::registrarSalida));
            pnlMenu.add(Box.createVerticalStrut(4));
        }

        if (esAdmin) {
            // ── Solo ADMIN ──
            pnlMenu.add(seccion("REPORTES"));
            pnlMenu.add(botonMenu("📊  Reporte por Empleado",
                    () -> abrir(new FrmReporteEmpleado())));
            pnlMenu.add(Box.createVerticalStrut(4));

            pnlMenu.add(seccion("GESTIÓN EMPLEADOS"));
            pnlMenu.add(botonMenu("👥  Lista de Empleados",
                    () -> abrir(new FrmEmpleados())));
            pnlMenu.add(botonMenu("➕  Nuevo Empleado",
                    () -> abrir(new FrmEmpleadoForm(null))));
            pnlMenu.add(Box.createVerticalStrut(4));

            pnlMenu.add(seccion("RECONOCIMIENTO FACIAL"));
            pnlMenu.add(botonMenu("📸  Registrar Foto Rostro",
                    () -> abrir(new PnlCapturarRostro())));
            pnlMenu.add(botonMenu("🔧  Diagnóstico Cámara",
                    () -> abrir(new PnlDiagnosticoCamara())));
            pnlMenu.add(Box.createVerticalStrut(4));
        }

        pnlMenu.add(Box.createVerticalGlue());
        pnlMenu.add(botonMenu("🚪  Cerrar Sesión", this::cerrarSesion));

        // ── Contenido central ──────────────────────────────────
        pnlContenido = new JPanel(new BorderLayout());
        pnlContenido.setBackground(Estilos.BLANCO);

        String bienvenida = esAdmin
            ? "<html><center><br><br>" +
              "<span style='font-size:20pt;color:#B40000;'><b>Bienvenido, Administrador</b></span><br><br>" +
              "<span style='font-size:11pt;color:#555;'>Tiene acceso completo al sistema.</span><br>" +
              "<span style='font-size:10pt;color:#888;'>Seleccione una opción del menú lateral.</span>" +
              "</center></html>"
            : "<html><center><br><br>" +
              "<span style='font-size:20pt;color:#B40000;'><b>Bienvenido, " + usuario + "</b></span><br><br>" +
              "<span style='font-size:11pt;color:#555;'>Puede consultar sus asistencias por fecha.</span><br><br>" +
              "<span style='font-size:10pt;color:#888;'>Para registrar su asistencia use el botón verde<br>" +
              "en la pantalla de inicio de sesión.</span>" +
              "</center></html>";

        JLabel lblBienvenida = new JLabel(bienvenida, SwingConstants.CENTER);
        lblBienvenida.setForeground(Estilos.NEGRO_TEXTO);
        pnlContenido.add(lblBienvenida, BorderLayout.CENTER);

        add(pnlTop,       BorderLayout.NORTH);
        add(pnlMenu,      BorderLayout.WEST);
        add(pnlContenido, BorderLayout.CENTER);
    }

    // ── Helpers menú ───────────────────────────────────────────
    private JLabel seccion(String texto) {
        JLabel l = new JLabel("  " + texto);
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(Color.BLACK);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        l.setBorder(BorderFactory.createEmptyBorder(10, 8, 2, 0));
        return l;
    }

    private JButton botonMenu(String texto, Runnable accion) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setForeground(Color.BLACK);
        btn.setBackground(Color.WHITE);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 8));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(Estilos.ROJO_UTA); }
            @Override public void mouseExited (MouseEvent e) { btn.setBackground(Color.WHITE); }
        });
        btn.addActionListener(e -> accion.run());
        return btn;
    }

    private void abrir(JPanel panel) {
        // Detener cámara del panel anterior si es PnlCapturarRostro o PnlDiagnosticoCamara
        Component[] componentes = pnlContenido.getComponents();
        for (Component comp : componentes) {
            if (comp instanceof PnlCapturarRostro) {
                ((PnlCapturarRostro) comp).forzarDetenerCamara();
            } else if (comp instanceof PnlDiagnosticoCamara) {
                ((PnlDiagnosticoCamara) comp).forzarDetenerCamara();
            }
        }
        
        pnlContenido.removeAll();
        pnlContenido.add(panel, BorderLayout.CENTER);
        pnlContenido.revalidate();
        pnlContenido.repaint();
    }

    private void cerrarSesion() {
        // Detener cámara del panel actual antes de cerrar sesión
        Component[] componentes = pnlContenido.getComponents();
        for (Component comp : componentes) {
            if (comp instanceof PnlCapturarRostro) {
                ((PnlCapturarRostro) comp).forzarDetenerCamara();
            } else if (comp instanceof PnlDiagnosticoCamara) {
                ((PnlDiagnosticoCamara) comp).forzarDetenerCamara();
            }
        }
        
        int op = JOptionPane.showConfirmDialog(this,
            "¿Desea cerrar la sesión?", "Cerrar Sesión",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (op == JOptionPane.YES_OPTION) {
            Sesion.getInstance().cerrar();
            new FrmLogin().setVisible(true);
            dispose();
        }
    }

    private void iniciarReloj() {
        Timer t = new Timer(1000, e ->
            lblReloj.setText(
                LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm:ss")) + "   "));
        t.start();
    }

    // ── Registro de Asistencia ───────────────────────────────────
    private void registrarEntrada() {
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override protected String doInBackground() throws Exception {
                Usuario usuario = Sesion.getInstance().getUsuarioActual();
                EmpleadoDAO empDao = new EmpleadoDAO();
                Empleado empleado = empDao.buscarPorIdUsuario(usuario.getUsuId());
                
                if (empleado == null) {
                    throw new Exception("No se encontró el empleado asociado a su cuenta de usuario.");
                }
                
                AsistenciaDAO asiDao = new AsistenciaDAO();
                return asiDao.registrarMarca(empleado.getEmpId());
            }
            @Override protected void done() {
                try {
                    String resultado = get();
                    JOptionPane.showMessageDialog(FrmPrincipal.this,
                        resultado,
                        "Registro de Asistencia",
                        JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(FrmPrincipal.this,
                        ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void registrarSalida() {
        // Ambos métodos usan la misma lógica de registrarMarca()
        // que detecta automáticamente qué marca registrar según la hora
        registrarEntrada();
    }
}
