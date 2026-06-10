package sistema.view;

import org.bytedeco.javacv.*;
import sistema.util.Estilos;
import sistema.util.ReconocimientoFacialService;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Panel de diagnóstico de cámara.
 * Permite al administrador verificar qué cámaras están disponibles
 * y cuál funciona correctamente antes de registrar fotos.
 * Accesible desde: Menú → RECONOCIMIENTO FACIAL → Diagnóstico Cámara
 */
public class PnlDiagnosticoCamara extends JPanel {

    private JLabel        lblCamara;
    private JLabel        lblEstado;
    private JComboBox<String> cboCamara;
    private JButton       btnProbar;
    private JButton       btnDetener;
    private JButton       btnEscanear;
    private JTextArea     txtLog;

    private FrameGrabber  grabber;
    private Timer         timerPreview;
    private boolean       camaraActiva = false;
    private ReconocimientoFacialService servicio;

    public PnlDiagnosticoCamara() {
        this.servicio = ReconocimientoFacialService.getInstance();
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));
        setBackground(Estilos.BLANCO);

        // ── Cabecera ──────────────────────────────────────────
        JPanel pnlTop = new JPanel(new BorderLayout());
        pnlTop.setBackground(Estilos.ROJO_UTA);
        pnlTop.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        JLabel lblTit = new JLabel("Diagnóstico de Cámara");
        lblTit.setFont(Estilos.FUENTE_TITULO);
        lblTit.setForeground(Estilos.BLANCO);
        JLabel lblSub = new JLabel("Verifique que la cámara funcione antes de registrar fotos");
        lblSub.setFont(Estilos.FUENTE_PEQUENA);
        lblSub.setForeground(Estilos.BLANCO);
        JPanel pnlTitles = new JPanel(new GridLayout(2, 1));
        pnlTitles.setOpaque(false);
        pnlTitles.add(lblTit);
        pnlTitles.add(lblSub);
        pnlTop.add(pnlTitles, BorderLayout.WEST);

        // ── Panel de controles ────────────────────────────────
        JPanel pnlControles = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        pnlControles.setBackground(Estilos.BLANCO);
        pnlControles.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Estilos.GRIS_BORDE));

        pnlControles.add(Estilos.etiquetaFormulario("Cámara índice:"));

        cboCamara = new JComboBox<>(new String[]{
            "0 — Cámara integrada (laptop)",
            "1 — Webcam externa USB",
            "2 — Segunda cámara externa",
            "3 — Tercera cámara externa"
        });
        Estilos.estilizarCombo(cboCamara);
        cboCamara.setPreferredSize(new Dimension(260, 28));
        pnlControles.add(cboCamara);

        btnProbar = new JButton("Probar");
        Estilos.estilizarBotonPrimario(btnProbar);
        btnProbar.setPreferredSize(new Dimension(110, 30));
        pnlControles.add(btnProbar);

        btnDetener = new JButton("Detener");
        Estilos.estilizarBotonSecundario(btnDetener);
        btnDetener.setPreferredSize(new Dimension(110, 30));
        btnDetener.setEnabled(false);
        pnlControles.add(btnDetener);

        btnEscanear = new JButton("Escanear todas");
        Estilos.estilizarBotonSecundario(btnEscanear);
        btnEscanear.setPreferredSize(new Dimension(150, 30));
        pnlControles.add(btnEscanear);

        // ── Panel central: preview + log ─────────────────────
        JPanel pnlCentral = new JPanel(new GridLayout(1, 2, 12, 0));
        pnlCentral.setBackground(Estilos.BLANCO);
        pnlCentral.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Vista de la cámara
        JPanel pnlPreview = new JPanel(new BorderLayout(4, 4));
        pnlPreview.setBackground(Estilos.BLANCO);
        JLabel lblTitCam = new JLabel("Vista en tiempo real", SwingConstants.CENTER);
        lblTitCam.setFont(Estilos.FUENTE_SUBTIT);
        lblTitCam.setForeground(Estilos.NEGRO_TEXTO);

        lblCamara = new JLabel(
            "<html><center><br><br><br><br>Presione 'Probar'<br>para iniciar</center></html>",
            SwingConstants.CENTER);
        lblCamara.setPreferredSize(new Dimension(320, 240));
        lblCamara.setBackground(Color.BLACK);
        lblCamara.setForeground(Color.GRAY);
        lblCamara.setOpaque(true);

        JPanel wrapCam = new JPanel(new BorderLayout());
        wrapCam.setBorder(BorderFactory.createLineBorder(Estilos.ROJO_UTA, 2));
        wrapCam.setBackground(Color.BLACK);
        wrapCam.add(lblCamara, BorderLayout.CENTER);

        pnlPreview.add(lblTitCam, BorderLayout.NORTH);
        pnlPreview.add(wrapCam,   BorderLayout.CENTER);

        // Log de diagnóstico
        JPanel pnlLog = new JPanel(new BorderLayout(4, 4));
        pnlLog.setBackground(Estilos.BLANCO);
        JLabel lblTitLog = new JLabel("Registro de diagnóstico", SwingConstants.CENTER);
        lblTitLog.setFont(Estilos.FUENTE_SUBTIT);
        lblTitLog.setForeground(Estilos.NEGRO_TEXTO);

        txtLog = new JTextArea();
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 11));
        txtLog.setEditable(false);
        Estilos.estilizarAreaTexto(txtLog);
        txtLog.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        txtLog.setText("=== Diagnóstico de Cámara — UTA ===\n\n" +
                       "Presione 'Probar' para verificar la cámara seleccionada,\n" +
                       "o 'Escanear todas' para detectar las disponibles.\n");

        JScrollPane scrollLog = new JScrollPane(txtLog);
        scrollLog.setBorder(BorderFactory.createLineBorder(Estilos.GRIS_BORDE));

        pnlLog.add(lblTitLog,  BorderLayout.NORTH);
        pnlLog.add(scrollLog,  BorderLayout.CENTER);

        pnlCentral.add(pnlPreview);
        pnlCentral.add(pnlLog);

        // ── Estado inferior ───────────────────────────────────
        lblEstado = new JLabel("  Seleccione una cámara y presione Probar.",
                SwingConstants.LEFT);
        lblEstado.setFont(Estilos.FUENTE_NORMAL);
        lblEstado.setForeground(Estilos.NEGRO_TEXTO);
        lblEstado.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Estilos.GRIS_BORDE),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));

        add(pnlTop,       BorderLayout.NORTH);
        JPanel pnlMain = new JPanel(new BorderLayout());
        pnlMain.add(pnlControles, BorderLayout.NORTH);
        pnlMain.add(pnlCentral,   BorderLayout.CENTER);
        add(pnlMain,      BorderLayout.CENTER);
        add(lblEstado,    BorderLayout.SOUTH);

        // ── Eventos ──────────────────────────────────────────
        btnProbar.addActionListener(e -> probarCamara());
        btnDetener.addActionListener(e -> detenerCamara());
        btnEscanear.addActionListener(e -> escanearCamaras());

        // Detener cámara al cambiar de panel
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0
                    && !isShowing()) {
                detenerCamara();
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    // PROBAR CÁMARA
    // ─────────────────────────────────────────────────────────
    private void probarCamara() {
        if (camaraActiva) detenerCamara();
        int indice = cboCamara.getSelectedIndex();
        log("Intentando abrir cámara " + indice + "...");
        setEstado("Abriendo cámara " + indice + "...", Color.DARK_GRAY);
        btnProbar.setEnabled(false);

        SwingWorker<Void, Void> w = new SwingWorker<>() {
            @Override protected Void doInBackground() throws Exception {
                grabber = servicio.abrirCamara(indice);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    camaraActiva = true;
                    btnDetener.setEnabled(true);
                    log("Cámara " + indice + " abierta correctamente.");
                    log("  Resolución: 640 x 480");
                    setEstado("Cámara " + indice + " activa — Reconocimiento facial disponible.",
                            Estilos.VERDE_OK);
                    iniciarPreview();
                } catch (Exception ex) {
                    btnProbar.setEnabled(true);
                    log("Error cámara " + indice + ": " + ex.getMessage());
                    log("  Verifique la conexión de la cámara.");
                    setEstado("No se pudo abrir la cámara " + indice + ".", Color.RED);
                    lblCamara.setText("<html><center><font color='gray'>Sin cámara</font></center></html>");
                    lblCamara.repaint();
                }
            }
        };
        w.execute();
    }

    private void iniciarPreview() {
        timerPreview = new Timer(66, e -> {
            if (!camaraActiva || grabber == null) return;
            try {
                org.bytedeco.javacv.Frame frame = grabber.grab();
                if (frame == null || frame.image == null) return;
                
                BufferedImage img = null;
                try {
                    // Intentar convertir directamente sin detección de rostros para evitar errores
                    img = servicio.frameABufferedImage(frame);
                } catch (Exception ex) {
                    // Si falla la conversión, continuar al siguiente frame
                    return;
                }
                
                if (img != null) {
                    Image scaled = img.getScaledInstance(320, 240, Image.SCALE_FAST);
                    SwingUtilities.invokeLater(() -> {
                        lblCamara.setIcon(new ImageIcon(scaled));
                        lblCamara.setText("");
                        lblCamara.repaint();
                    });
                }
            } catch (Exception ex) {
                // Silenciar completamente todos los errores para no saturar la consola
            }
        });
        timerPreview.setRepeats(true);
        timerPreview.start();
    }

    private void detenerCamara() {
        if (timerPreview != null) timerPreview.stop();
        servicio.cerrarCamara(grabber);
        grabber = null;
        camaraActiva = false;
        lblCamara.setIcon(null);
        lblCamara.setText("<html><center><font color='gray'><br>Cámara detenida</font></center></html>");
        lblCamara.repaint();
        btnProbar.setEnabled(true);
        btnDetener.setEnabled(false);
        setEstado("Cámara detenida.", Color.DARK_GRAY);
        log("Cámara detenida.");
    }

    public void forzarDetenerCamara() {
        System.out.println("[PnlDiagnosticoCamara] forzarDetenerCamara() llamado - camaraActiva: " + camaraActiva);
        if (camaraActiva) detenerCamara();
    }

    // ─────────────────────────────────────────────────────────
    // ESCANEAR TODAS LAS CÁMARAS
    // ─────────────────────────────────────────────────────────
    private void escanearCamaras() {
        if (camaraActiva) detenerCamara();
        btnEscanear.setEnabled(false);
        log("\n=== Escaneando cámaras disponibles (0-3) ===");
        setEstado("Escaneando cámaras... Por favor espere.", Color.DARK_GRAY);

        SwingWorker<Void, String> w = new SwingWorker<>() {
            @Override protected Void doInBackground() {
                int encontradas = 0;
                for (int i = 0; i <= 3; i++) {
                    final int idx = i;
                    publish("Probando cámara " + idx + "...");
                    FrameGrabber g = null;
                    try {
                        g = new OpenCVFrameGrabber(idx);
                        g.setImageWidth(320);
                        g.setImageHeight(240);
                        g.start();
                        org.bytedeco.javacv.Frame f = g.grab();
                        if (f != null && f.imageWidth > 0) {
                            publish("  ✔ Cámara " + idx + " disponible — " +
                                    f.imageWidth + "x" + f.imageHeight);
                            encontradas++;
                        }
                        g.stop();
                        g.release();
                    } catch (Exception e) {
                        publish("  ✗ Cámara " + idx + " no disponible.");
                        if (g != null) try { g.release(); } catch (Exception ex2) {}
                    }
                }
                publish("\nTotal cámaras encontradas: " + encontradas);
                if (encontradas == 0) {
                    publish("No se detectó ninguna cámara.");
                    publish("  Verifique que la webcam esté conectada y los drivers instalados.");
                }
                return null;
            }

            @Override protected void process(java.util.List<String> chunks) {
                chunks.forEach(msg -> log(msg));
            }

            @Override protected void done() {
                btnEscanear.setEnabled(true);
                setEstado("Escaneo completado. Revise el log.", Color.DARK_GRAY);
            }
        };
        w.execute();
    }

    // ── Helpers ──────────────────────────────────────────────
    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append(msg + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }

    private void setEstado(String msg, Color color) {
        SwingUtilities.invokeLater(() -> {
            lblEstado.setText("  " + msg);
            lblEstado.setForeground(color);
        });
    }
}
