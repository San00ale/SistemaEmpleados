package sistema.view;

import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.Mat;
import sistema.dao.EmpleadoDAO;
import sistema.dao.RostroDAO;
import sistema.model.Empleado;
import sistema.util.Estilos;
import sistema.util.ReconocimientoFacialService;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Panel para registrar la foto del rostro de un empleado.
 * Solo accesible para ADMIN desde el formulario de empleados.
 *
 * Flujo:
 *  1. El admin busca al empleado por cédula.
 *  2. Se abre la webcam con preview en tiempo real.
 *  3. El admin presiona "Capturar Foto".
 *  4. El sistema detecta el rostro, recorta y guarda en BD.
 */
public class PnlCapturarRostro extends JPanel {

    // ── UI ──────────────────────────────────────────────────────
    private JTextField  txtCedula;
    private JLabel      lblNombre;
    private JLabel      lblEstado;
    private JLabel      lblCamara;
    private JLabel      lblFotoCapturada;
    private JButton     btnBuscar;
    private JButton     btnCapturar;
    private JButton     btnGuardar;
    private JButton     btnEliminarFoto;

    // ── Estado ──────────────────────────────────────────────────
    private FrameGrabber  grabber;
    private Timer         timerPreview;
    private boolean       camaraActiva = false;
    private Empleado      empleadoActual;
    private byte[]        fotoCapturada;      // bytes JPEG listos para guardar
    private ReconocimientoFacialService servicio;

    public PnlCapturarRostro() {
        this.servicio = ReconocimientoFacialService.getInstance();
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));
        setBackground(Estilos.BLANCO);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // ── Cabecera ──
        JPanel pnlTop = new JPanel(new BorderLayout());
        pnlTop.setBackground(Estilos.ROJO_UTA);
        pnlTop.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        JLabel lblTitulo = new JLabel("Registro de Foto para Reconocimiento Facial");
        lblTitulo.setFont(Estilos.FUENTE_TITULO);
        lblTitulo.setForeground(Estilos.BLANCO);
        pnlTop.add(lblTitulo, BorderLayout.WEST);

        // ── Búsqueda de empleado ──
        JPanel pnlBuscar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        pnlBuscar.setBackground(Estilos.BLANCO);
        pnlBuscar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Estilos.GRIS_BORDE));

        pnlBuscar.add(Estilos.etiquetaFormulario("Cédula del empleado:"));
        txtCedula = new JTextField(14);
        Estilos.estilizarCampo(txtCedula);
        pnlBuscar.add(txtCedula);

        btnBuscar = new JButton("Buscar");
        Estilos.estilizarBotonPrimario(btnBuscar);
        pnlBuscar.add(btnBuscar);

        lblNombre = new JLabel("—");
        lblNombre.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblNombre.setForeground(Estilos.NEGRO_TEXTO);
        pnlBuscar.add(lblNombre);

        // ── Panel central: cámara + foto capturada ──
        JPanel pnlCentral = new JPanel(new GridLayout(1, 2, 12, 0));
        pnlCentral.setBackground(Estilos.BLANCO);

        // Cámara en vivo
        JPanel pnlCam = new JPanel(new BorderLayout(4, 4));
        pnlCam.setBackground(Estilos.BLANCO);
        JLabel lblTitCam = new JLabel("Vista en tiempo real", SwingConstants.CENTER);
        lblTitCam.setFont(Estilos.FUENTE_SUBTIT);
        lblTitCam.setForeground(Estilos.NEGRO_TEXTO);

        lblCamara = new JLabel("Iniciando...", SwingConstants.CENTER);
        lblCamara.setPreferredSize(new Dimension(310, 240));
        lblCamara.setBackground(Color.BLACK);
        lblCamara.setOpaque(true);
        lblCamara.setForeground(Color.GRAY);
        JPanel wrapCam = new JPanel(new BorderLayout());
        wrapCam.setBorder(BorderFactory.createLineBorder(Estilos.ROJO_UTA, 2));
        wrapCam.setBackground(Color.BLACK);
        wrapCam.add(lblCamara, BorderLayout.CENTER);

        pnlCam.add(lblTitCam,  BorderLayout.NORTH);
        pnlCam.add(wrapCam,    BorderLayout.CENTER);

        // Foto capturada (preview)
        JPanel pnlFoto = new JPanel(new BorderLayout(4, 4));
        pnlFoto.setBackground(Estilos.BLANCO);
        JLabel lblTitFoto = new JLabel("Foto a guardar", SwingConstants.CENTER);
        lblTitFoto.setFont(Estilos.FUENTE_SUBTIT);
        lblTitFoto.setForeground(Estilos.NEGRO_TEXTO);

        lblFotoCapturada = new JLabel("<html><center><br><br>Sin captura</center></html>",
                SwingConstants.CENTER);
        lblFotoCapturada.setPreferredSize(new Dimension(310, 240));
        lblFotoCapturada.setBackground(new Color(240, 240, 240));
        lblFotoCapturada.setOpaque(true);
        lblFotoCapturada.setForeground(Color.GRAY);
        JPanel wrapFoto = new JPanel(new BorderLayout());
        wrapFoto.setBorder(BorderFactory.createLineBorder(Estilos.GRIS_BORDE, 2));
        wrapFoto.setBackground(new Color(240, 240, 240));
        wrapFoto.add(lblFotoCapturada, BorderLayout.CENTER);

        pnlFoto.add(lblTitFoto,   BorderLayout.NORTH);
        pnlFoto.add(wrapFoto,     BorderLayout.CENTER);

        pnlCentral.add(pnlCam);
        pnlCentral.add(pnlFoto);

        // ── Panel inferior: botones + estado ──
        JPanel pnlSur = new JPanel(new BorderLayout(4, 4));
        pnlSur.setBackground(Estilos.BLANCO);

        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        pnlBtns.setBackground(Estilos.BLANCO);
        pnlBtns.setOpaque(true);

        btnCapturar = new JButton("Capturar Foto");
        btnCapturar.setPreferredSize(new Dimension(160, 36));
        btnCapturar.setEnabled(true); // Habilitado para pruebas
        btnCapturar.setToolTipText("Haga click para capturar foto del rostro");
        btnCapturar.setBackground(new Color(220, 53, 69));
        btnCapturar.setForeground(Color.WHITE);
        btnCapturar.setFocusPainted(false);

        btnGuardar = new JButton("Guardar en BD");
        btnGuardar.setPreferredSize(new Dimension(150, 36));
        btnGuardar.setEnabled(true); // Habilitado para pruebas
        btnGuardar.setBackground(Estilos.VERDE_OK);
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setFocusPainted(false);
        btnGuardar.setToolTipText("Haga click para guardar la foto en la base de datos");

        btnEliminarFoto = new JButton(" Eliminar foto");
        btnEliminarFoto.setPreferredSize(new Dimension(140, 36));
        btnEliminarFoto.setEnabled(true); // Habilitado para pruebas
        btnEliminarFoto.setBackground(new Color(108, 117, 125));
        btnEliminarFoto.setForeground(Color.WHITE);
        btnEliminarFoto.setFocusPainted(false);
        btnEliminarFoto.setToolTipText("Haga click para eliminar la foto registrada");

        pnlBtns.add(btnCapturar);
        pnlBtns.add(btnGuardar);
        pnlBtns.add(btnEliminarFoto);
        
        // Asegurar que los botones estén arriba en el z-order
        pnlBtns.setComponentZOrder(btnCapturar, 0);
        pnlBtns.setComponentZOrder(btnGuardar, 1);
        pnlBtns.setComponentZOrder(btnEliminarFoto, 2);

        lblEstado = new JLabel(
            "  Busque un empleado e inicie la cámara para registrar su foto.",
            SwingConstants.LEFT);
        lblEstado.setFont(Estilos.FUENTE_NORMAL);
        lblEstado.setForeground(Estilos.NEGRO_TEXTO);
        lblEstado.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        // Nota
        JLabel lblNota = new JLabel(
            "<html><i>Consejos: buena iluminación frontal, mire directo a la cámara, " +
            "sin lentes de sol ni objetos que cubran el rostro.</i></html>",
            SwingConstants.CENTER);
        lblNota.setFont(Estilos.FUENTE_PEQUENA);
        lblNota.setForeground(Estilos.NEGRO_TEXTO);

        pnlSur.add(pnlBtns,  BorderLayout.NORTH);
        pnlSur.add(lblEstado, BorderLayout.CENTER);
        pnlSur.add(lblNota,   BorderLayout.SOUTH);
        
        // Asegurar que pnlSur sea opaco para evitar problemas de transparencia
        pnlSur.setOpaque(true);

        add(pnlTop,     BorderLayout.NORTH);
        add(pnlBuscar,  BorderLayout.NORTH); // Se reemplazará abajo

        // Reorganizar: top + buscar como Norte, central como centro, sur como sur
        JPanel pnlNorte = new JPanel(new BorderLayout());
        pnlNorte.add(pnlTop,    BorderLayout.NORTH);
        pnlNorte.add(pnlBuscar, BorderLayout.SOUTH);

        removeAll();
        add(pnlNorte,   BorderLayout.NORTH);
        add(pnlCentral, BorderLayout.CENTER);
        add(pnlSur,     BorderLayout.SOUTH);

        // ── Eventos ──
        btnBuscar.addActionListener(e -> buscarEmpleado());
        txtCedula.addActionListener(e -> buscarEmpleado());
        btnCapturar.addActionListener(e -> {
            System.out.println("[PnlCapturarRostro] btnCapturar click recibido");
            capturarFoto();
        });
        btnGuardar.addActionListener(e -> {
            System.out.println("[PnlCapturarRostro] btnGuardar click recibido");
            guardarFoto();
        });
        btnEliminarFoto.addActionListener(e -> {
            System.out.println("[PnlCapturarRostro] btnEliminarFoto click recibido");
            eliminarFoto();
        });

        // Iniciar cámara al mostrarse
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) {
                    System.out.println("[PnlCapturarRostro] Panel mostrado, iniciando cámara");
                    iniciarCamara();
                } else {
                    System.out.println("[PnlCapturarRostro] Panel ocultado, deteniendo cámara");
                    detenerCamara();
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    // BÚSQUEDA
    // ─────────────────────────────────────────────────────────────
    private void buscarEmpleado() {
        String cedula = txtCedula.getText().trim();
        if (cedula.isEmpty()) { alerta("Ingrese la cédula."); return; }
        try {
            Empleado emp = new EmpleadoDAO().buscarPorCedula(cedula);
            if (emp == null || !emp.isEmpActivo()) {
                setEstado("Empleado no encontrado o inactivo.", Color.RED);
                lblNombre.setText("—");
                btnCapturar.setEnabled(false);
                btnEliminarFoto.setEnabled(false);
                return;
            }
            empleadoActual = emp;
            lblNombre.setText("✔  " + emp.getNombreCompleto() + "  [" + emp.getEmpTipoJornada() + "]");

            boolean tieneFoto = new RostroDAO().tieneFoto(emp.getEmpId());
            btnCapturar.setEnabled(camaraActiva);
            btnEliminarFoto.setEnabled(tieneFoto);
            fotoCapturada = null;
            btnGuardar.setEnabled(false);
            lblFotoCapturada.setIcon(null);
            lblFotoCapturada.setText(tieneFoto
                ? "<html><center>Ya tiene foto<br>registrada</center></html>"
                : "<html><center>Sin captura</center></html>");
            lblFotoCapturada.repaint();

            setEstado(tieneFoto
                ? "Este empleado ya tiene foto. Capture una nueva para reemplazarla."
                : "Empleado listo. Presione 'Capturar Foto'.", Estilos.VERDE_OK);
        } catch (Exception ex) {
            setEstado("Error: " + ex.getMessage(), Color.RED);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // CÁMARA
    // ─────────────────────────────────────────────────────────────
    private void iniciarCamara() {
        if (camaraActiva) return;
        SwingWorker<Void, Void> w = new SwingWorker<>() {
            @Override protected Void doInBackground() throws Exception {
                grabber = servicio.abrirCamara();
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    camaraActiva = true;
                    setEstado("Cámara activa.", Estilos.VERDE_OK);
                    if (empleadoActual != null) btnCapturar.setEnabled(true);
                    timerPreview = new Timer(66, e -> actualizarPreview());
                    timerPreview.setRepeats(true);
                    timerPreview.start();
                    System.out.println("[PnlCapturarRostro] Timer iniciado, cámara activa: " + camaraActiva);
                } catch (Exception ex) {
                    setEstado("No se pudo abrir la cámara integrada.", Color.RED);
                    lblCamara.setText("<html><center><font color='gray'>Sin cámara</font></center></html>");
                    lblCamara.repaint();
                    System.err.println("[PnlCapturarRostro] Error al iniciar cámara: " + ex.getMessage());
                }
            }
        };
        w.execute();
    }

    private void detenerCamara() {
        System.out.println("[PnlCapturarRostro] detenerCamara() llamado - camaraActiva: " + camaraActiva + ", grabber: " + (grabber != null));
        if (timerPreview != null) {
            timerPreview.stop();
            timerPreview = null;
        }
        if (grabber != null) {
            try {
                servicio.cerrarCamara(grabber);
                System.out.println("[PnlCapturarRostro] Cámara cerrada correctamente");
                // Agregar delay de 2 segundos para asegurar que Windows libere completamente la cámara
                Thread.sleep(2000);
            } catch (Exception e) {
                System.err.println("[PnlCapturarRostro] Error al cerrar cámara: " + e.getMessage());
            }
            grabber = null;
        }
        camaraActiva = false;
        btnCapturar.setEnabled(false);
        System.out.println("[PnlCapturarRostro] detenerCamara() completado");
    }

    public void forzarDetenerCamara() {
        System.out.println("[PnlCapturarRostro] forzarDetenerCamara() llamado - camaraActiva: " + camaraActiva);
        detenerCamara();
    }

    private void actualizarPreview() {
        if (!camaraActiva || grabber == null) return;
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
            System.err.println("[PnlCapturarRostro] Error en actualizarPreview: " + ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // CAPTURA Y GUARDADO
    // ─────────────────────────────────────────────────────────────
    private void capturarFoto() {
        System.out.println("[PnlCapturarRostro] capturarFoto() llamado - camaraActiva: " + camaraActiva + ", grabber: " + (grabber != null) + ", empleadoActual: " + (empleadoActual != null));
        if (!camaraActiva || grabber == null) { alerta("Cámara no disponible."); return; }
        if (empleadoActual == null) { alerta("Busque un empleado primero."); return; }

        setEstado("Capturando...", Color.DARK_GRAY);

        SwingWorker<byte[], Void> w = new SwingWorker<>() {
            @Override protected byte[] doInBackground() throws Exception {
                // Tomar 3 frames y quedarse con el que tenga rostro detectado
                for (int i = 0; i < 5; i++) {
                    org.bytedeco.javacv.Frame frame = grabber.grab();
                    Mat rostro = servicio.detectarPrimerRostro(frame);
                    if (rostro != null) {
                        byte[] bytes = servicio.matABytes(rostro);
                        rostro.deallocate();
                        return bytes;
                    }
                    Thread.sleep(100);
                }
                return null;
            }
            @Override protected void done() {
                try {
                    fotoCapturada = get();
                    if (fotoCapturada == null) {
                        setEstado("No se detectó ningún rostro. Mire directamente a la cámara.",
                                Color.RED);
                        return;
                    }
                    // Mostrar preview de la foto capturada
                    ImageIcon icon = new ImageIcon(fotoCapturada);
                    Image scaled = icon.getImage().getScaledInstance(200, 180, Image.SCALE_SMOOTH);
                    lblFotoCapturada.setIcon(new ImageIcon(scaled));
                    lblFotoCapturada.setText("");
                    lblFotoCapturada.repaint();
                    btnGuardar.setEnabled(true);
                    setEstado("Rostro capturado. Presione 'Guardar en BD' para confirmar.",
                            Estilos.VERDE_OK);
                } catch (Exception ex) {
                    setEstado("Error al capturar: " + ex.getMessage(), Color.RED);
                    ex.printStackTrace();
                }
            }
        };
        w.execute();
    }

    private void guardarFoto() {
        System.out.println("[PnlCapturarRostro] guardarFoto() llamado - fotoCapturada: " + (fotoCapturada != null) + ", empleadoActual: " + (empleadoActual != null));
        if (fotoCapturada == null || empleadoActual == null) {
            System.out.println("[PnlCapturarRostro] guardarFoto() abortado - fotoCapturada o empleadoActual es null");
            return;
        }
        try {
            if (!new RostroDAO().guardarFoto(empleadoActual.getEmpId(), fotoCapturada)) {
                alerta("No se pudo guardar la foto en la base de datos.");
                return;
            }
            JOptionPane.showMessageDialog(this,
                "Foto guardada correctamente para:\n" + empleadoActual.getNombreCompleto(),
                "Éxito", JOptionPane.INFORMATION_MESSAGE);
            fotoCapturada = null;
            btnGuardar.setEnabled(false);
            btnEliminarFoto.setEnabled(true);
            setEstado("Foto registrada exitosamente.", Estilos.VERDE_OK);
        } catch (Exception ex) {
            alerta("Error al guardar: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void eliminarFoto() {
        System.out.println("[PnlCapturarRostro] eliminarFoto() llamado - empleadoActual: " + (empleadoActual != null));
        if (empleadoActual == null) {
            System.out.println("[PnlCapturarRostro] eliminarFoto() abortado - empleadoActual es null");
            return;
        }
        int op = JOptionPane.showConfirmDialog(this,
            "¿Eliminar la foto registrada de:\n" + empleadoActual.getNombreCompleto() + "?",
            "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (op == JOptionPane.YES_OPTION) {
            try {
                new RostroDAO().eliminarFoto(empleadoActual.getEmpId());
                lblFotoCapturada.setIcon(null);
                lblFotoCapturada.setText("<html><center>Sin foto</center></html>");
                lblFotoCapturada.repaint();
                btnEliminarFoto.setEnabled(false);
                btnGuardar.setEnabled(false);
                setEstado("Foto eliminada.", Color.DARK_GRAY);
            } catch (Exception ex) {
                alerta("Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────

    private void setEstado(String msg, Color color) {
        SwingUtilities.invokeLater(() -> {
            lblEstado.setText("  " + msg);
            lblEstado.setForeground(color);
        });
    }

    private void alerta(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Aviso", JOptionPane.WARNING_MESSAGE);
    }
}
