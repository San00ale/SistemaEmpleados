package sistema.view;

import org.bytedeco.javacv.*;
import sistema.dao.RostroDAO;
import sistema.model.Empleado;
import sistema.model.Usuario;
import sistema.util.Estilos;
import sistema.util.ReconocimientoFacialService;
import sistema.util.ReconocimientoFacialService.ResultadoReconocimiento;
import sistema.util.Sesion;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Panel de login por reconocimiento facial.
 *
 * Flujo:
 *  1. Al abrirse, carga todos los empleados con foto desde la BD.
 *  2. Abre la webcam y muestra el video en tiempo real.
 *  3. El usuario presiona "Reconocer" (o automáticamente cada 2s).
 *  4. Si el rostro coincide → carga el usuario asociado → abre FrmPrincipal.
 *  5. Muestra feedback visual: rectángulo rojo sobre el rostro detectado.
 */
public class PnlLoginFacial extends JPanel {

    // ── UI ────────────────────────────────────────────────────────
    private JLabel        lblCamara;        // Muestra el feed de la cámara
    private JLabel        lblEstado;        // Mensaje de estado/resultado
    private JLabel        lblInstruccion;
    private JButton       btnReconocer;
    private JProgressBar  pbConfianza;

    // ── Estado ────────────────────────────────────────────────────
    private FrameGrabber  grabber;
    private Timer         timerPreview;     // refresca el preview a 15 fps
    private Timer         timerAutoRecon;   // intenta reconocer cada 2s automáticamente
    private boolean       camaraActiva = false;
    private boolean       reconociendo = false;
    private List<Empleado> empleadosConFoto;
    private ReconocimientoFacialService servicio;
    private int           errorCount = 0;    // contador de errores de cámara

    // Callback para notificar al FrmLogin cuando el reconocimiento fue exitoso
    private final Runnable onExito;

    public PnlLoginFacial(Runnable onExito) {
        this.onExito  = onExito;
        this.servicio = ReconocimientoFacialService.getInstance();
        initComponents();
        cargarEmpleados();
    }

    // ─────────────────────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────────────────────
    private void initComponents() {
        setLayout(new BorderLayout(0, 6));
        setBackground(Estilos.BLANCO);

        // ── Instrucción superior ──
        lblInstruccion = new JLabel(
            "<html><center>Colóquese frente a la cámara y presione<b> Reconocer</b>,<br>" +
            "o espere el reconocimiento automático.</center></html>",
            SwingConstants.CENTER);
        lblInstruccion.setFont(Estilos.FUENTE_PEQUENA);
        lblInstruccion.setForeground(Estilos.NEGRO_TEXTO);
        lblInstruccion.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        // ── Vista de la cámara ──
        lblCamara = new JLabel("", SwingConstants.CENTER);
        lblCamara.setPreferredSize(new Dimension(320, 240));
        lblCamara.setBackground(Color.BLACK);
        lblCamara.setOpaque(true);
        lblCamara.setText("<html><center><font color='gray'>" +
                "📷<br>Iniciando cámara...</font></center></html>");

        JPanel pnlCamara = new JPanel(new BorderLayout());
        pnlCamara.setBackground(Color.BLACK);
        pnlCamara.setBorder(BorderFactory.createLineBorder(Estilos.ROJO_UTA, 2));
        pnlCamara.add(lblCamara, BorderLayout.CENTER);

        // ── Barra de confianza ──
        pbConfianza = new JProgressBar(0, 100);
        pbConfianza.setStringPainted(true);
        pbConfianza.setString("Confianza: --");
        pbConfianza.setForeground(Estilos.ROJO_UTA);
        pbConfianza.setFont(Estilos.FUENTE_PEQUENA);
        pbConfianza.setValue(0);
        pbConfianza.setPreferredSize(new Dimension(0, 22));

        // ── Estado ──
        lblEstado = new JLabel("Cámara iniciando...", SwingConstants.CENTER);
        lblEstado.setFont(Estilos.FUENTE_NORMAL);
        lblEstado.setForeground(Estilos.NEGRO_TEXTO);

        // ── Botones ──
        btnReconocer = new JButton("🔍  Reconocer");
        Estilos.estilizarBotonPrimario(btnReconocer);
        btnReconocer.setPreferredSize(new Dimension(150, 34));
        btnReconocer.setEnabled(false);

        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        pnlBtns.setBackground(Estilos.BLANCO);
        pnlBtns.add(btnReconocer);

        // ── Panel inferior ──
        JPanel pnlSur = new JPanel(new BorderLayout(0, 3));
        pnlSur.setBackground(Estilos.BLANCO);
        pnlSur.add(pbConfianza, BorderLayout.NORTH);
        pnlSur.add(lblEstado,   BorderLayout.CENTER);
        pnlSur.add(pnlBtns,     BorderLayout.SOUTH);

        add(lblInstruccion, BorderLayout.NORTH);
        add(pnlCamara,      BorderLayout.CENTER);
        add(pnlSur,         BorderLayout.SOUTH);

        // ── Eventos ──
        btnReconocer.addActionListener(e -> ejecutarReconocimiento());

        // Iniciar cámara al mostrarse
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) iniciarCamara();
                else             detenerCamara();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    // CARGA DE EMPLEADOS
    // ─────────────────────────────────────────────────────────────
    private void cargarEmpleados() {
        SwingWorker<List<Empleado>, Void> worker = new SwingWorker<>() {
            @Override protected List<Empleado> doInBackground() throws Exception {
                return new RostroDAO().listarEmpleadosConFoto();
            }
            @Override protected void done() {
                try {
                    empleadosConFoto = get();
                    if (empleadosConFoto.isEmpty()) {
                        setEstado("⚠️ No hay empleados con foto registrada.", Color.ORANGE);
                    } else {
                        setEstado("✔ " + empleadosConFoto.size() + " empleado(s) cargados. " +
                                  "Mire a la cámara.", Estilos.VERDE_OK);
                    }
                } catch (Exception ex) {
                    setEstado("Error al cargar empleados: " + ex.getMessage(), Color.RED);
                }
            }
        };
        worker.execute();
    }

    // ─────────────────────────────────────────────────────────────
    // CÁMARA — INICIO / DETENCIÓN
    // ─────────────────────────────────────────────────────────────
    public void iniciarCamara() {
        if (camaraActiva) return;
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override protected Void doInBackground() throws Exception {
                // Agregar delay de 1 segundo para asegurar que Windows libere la cámara
                Thread.sleep(1000);
                grabber = servicio.abrirCamara();
                return null;
            }
            @Override protected void done() {
                try {
                    get(); // propaga excepciones
                    camaraActiva = true;
                    btnReconocer.setEnabled(true);
                    setEstado("✔ Cámara activa. Mire directamente al lente.", Estilos.VERDE_OK);
                    iniciarTimerPreview();
                    iniciarAutoReconocimiento();
                    System.out.println("[PnlLoginFacial] Timer iniciado, cámara activa: " + camaraActiva);
                } catch (Exception ex) {
                    setEstado("❌ No se pudo abrir la cámara integrada. Verifique la conexión.",
                              Color.RED);
                    lblCamara.setText("<html><center><font color='gray'>" +
                            "❌<br>Sin cámara</font></center></html>");
                    lblCamara.repaint();
                    System.err.println("[PnlLoginFacial] Error al iniciar cámara: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    public void detenerCamara() {
        if (timerPreview  != null) timerPreview.stop();
        if (timerAutoRecon != null) timerAutoRecon.stop();
        if (grabber != null) {
            try {
                servicio.cerrarCamara(grabber);
                System.out.println("[PnlLoginFacial] Cámara cerrada correctamente");
                // Agregar delay de 2 segundos para asegurar que Windows libere completamente la cámara
                Thread.sleep(2000);
            } catch (Exception e) {
                System.err.println("[PnlLoginFacial] Error al cerrar cámara: " + e.getMessage());
            }
            grabber = null;
        }
        camaraActiva = false;
        btnReconocer.setEnabled(false);
    }

    // ─────────────────────────────────────────────────────────────
    // TIMERS
    // ─────────────────────────────────────────────────────────────

    /** Refresca el preview a ~15 fps con detección de rostros visual. */
    private void iniciarTimerPreview() {
        timerPreview = new Timer(66, e -> actualizarPreview());
        timerPreview.setRepeats(true);
        timerPreview.start();
    }

    private void actualizarPreview() {
        if (!camaraActiva || grabber == null || reconociendo) return;
        try {
            org.bytedeco.javacv.Frame frame = grabber.grab();
            if (frame == null || frame.image == null) return;

            BufferedImage img = servicio.frameABufferedImage(frame);

            if (img != null) {
                Image scaled = img.getScaledInstance(lblCamara.getWidth(), lblCamara.getHeight(), Image.SCALE_FAST);
                SwingUtilities.invokeLater(() -> {
                    lblCamara.setIcon(new ImageIcon(scaled));
                    lblCamara.setText("");
                    lblCamara.repaint();
                });
            }
        } catch (Exception ex) {
            System.err.println("[PnlLoginFacial] Error en actualizarPreview: " + ex.getMessage());
            // Si hay error recurrente, reiniciar la cámara
            errorCount++;
            if (errorCount > 10) {
                System.err.println("[PnlLoginFacial] Demasiados errores, reiniciando cámara...");
                errorCount = 0;
                SwingUtilities.invokeLater(() -> {
                    detenerCamara();
                    iniciarCamara();
                });
            }
        }
    }

    /** Intenta reconocer automáticamente cada 2 segundos. */
    private void iniciarAutoReconocimiento() {
        timerAutoRecon = new Timer(2000, e -> {
            if (camaraActiva && !reconociendo
                    && empleadosConFoto != null && !empleadosConFoto.isEmpty()) {
                ejecutarReconocimiento();
            }
        });
        timerAutoRecon.start();
    }

    // ─────────────────────────────────────────────────────────────
    // RECONOCIMIENTO
    // ─────────────────────────────────────────────────────────────
    private void ejecutarReconocimiento() {
        if (!camaraActiva || grabber == null || reconociendo) return;
        if (empleadosConFoto == null || empleadosConFoto.isEmpty()) {
            setEstado("⚠️ No hay empleados con foto. Registre fotos primero.", Color.ORANGE);
            return;
        }

        reconociendo = true;
        btnReconocer.setEnabled(false);
        setEstado("🔍 Analizando rostro...", Color.DARK_GRAY);

        SwingWorker<ResultadoReconocimiento, Void> worker = new SwingWorker<>() {
            @Override
            protected ResultadoReconocimiento doInBackground() throws Exception {
                // Usar múltiples frames para mayor precisión
                return servicio.reconocerConReintentos(grabber, empleadosConFoto, 5);
            }

            @Override
            protected void done() {
                reconociendo = false;
                btnReconocer.setEnabled(true);
                try {
                    ResultadoReconocimiento resultado = get();
                    // Actualizar barra de confianza
                    int pct = (int)(resultado.similitud * 100);
                    pbConfianza.setValue(pct);
                    pbConfianza.setString(String.format("Confianza: %d%% (mín. %d%%)",
                            pct, (int)(ReconocimientoFacialService.UMBRAL_SIMILITUD * 100)));
                    pbConfianza.setForeground(resultado.reconocido
                            ? Estilos.VERDE_OK : Estilos.ROJO_ERROR);

                    if (resultado.reconocido) {
                        timerAutoRecon.stop();
                        setEstado(resultado.mensaje, Estilos.VERDE_OK);
                        autenticarPorFacial(resultado.empleado);
                    } else {
                        setEstado(resultado.mensaje, Estilos.ROJO_ERROR);
                    }
                } catch (Exception ex) {
                    setEstado("Error en reconocimiento: " + ex.getMessage(), Color.RED);
                }
            }
        };
        worker.execute();
    }

    /** Una vez reconocido el empleado, busca su usuario y abre la sesión. */
    private void autenticarPorFacial(Empleado empleado) {
        SwingWorker<Usuario, Void> worker = new SwingWorker<>() {
            @Override protected Usuario doInBackground() throws Exception {
                return new RostroDAO().obtenerUsuarioPorEmpleado(empleado.getEmpId());
            }
            @Override protected void done() {
                try {
                    Usuario u = get();
                    if (u == null) {
                        setEstado("⚠️ El empleado no tiene cuenta de acceso asignada.", Color.ORANGE);
                        if (timerAutoRecon != null) timerAutoRecon.restart();
                        return;
                    }
                    // Pausa visual de 1 segundo para que el usuario vea el mensaje
                    Timer pausa = new Timer(1200, ev -> {
                        detenerCamara();
                        Sesion.getInstance().iniciar(u);
                        onExito.run();   // Notifica a FrmLogin → abre FrmPrincipal
                    });
                    pausa.setRepeats(false);
                    pausa.start();
                } catch (Exception ex) {
                    setEstado("Error al obtener usuario: " + ex.getMessage(), Color.RED);
                }
            }
        };
        worker.execute();
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────
    private void setEstado(String msg, Color color) {
        SwingUtilities.invokeLater(() -> {
            lblEstado.setText(msg);
            lblEstado.setForeground(color);
        });
    }
}
