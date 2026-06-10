package sistema.view;

import sistema.dao.AsistenciaDAO;
import sistema.dao.EmpleadoDAO;
import sistema.model.Asistencia;
import sistema.model.Empleado;
import sistema.util.Estilos;
import sistema.util.Sesion;
import sistema.util.Utilidades;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Panel de consulta de asistencias propias para el empleado.
 * El empleado ingresa su cédula y una fecha para ver su entrada/salida.
 *
 * NOTA: El REGISTRO de asistencia se hace desde la pantalla de Login
 *       con el botón verde "REGISTRAR ASISTENCIA" — no desde aquí.
 *
 * EMPLEADO: Solo puede ver sus propias asistencias (cédula automática).
 * ADMIN: Puede buscar asistencias de cualquier empleado por cédula.
 */
public class FrmRegistroAsistencia extends JPanel {

    private JTextField        txtCedula;
    private JTextField        txtFecha;
    private JLabel            lblNombre, lblTipo, lblEstado;
    private JTable            tabla;
    private DefaultTableModel modelo;
    private boolean          esAdmin;
    private Empleado         empleadoActual;

    public FrmRegistroAsistencia() {
        esAdmin = Sesion.getInstance().isAdmin();
        initComponents();
        if (!esAdmin) {
            cargarEmpleadoActual();
        }
    }

    private void cargarEmpleadoActual() {
        try {
            int idUsuario = Sesion.getInstance().getUsuarioActual().getUsuId();
            empleadoActual = new EmpleadoDAO().buscarPorIdUsuario(idUsuario);
            if (empleadoActual != null) {
                txtCedula.setText(empleadoActual.getEmpCedula());
                txtCedula.setEditable(false);
                txtCedula.setBackground(new Color(240, 240, 240));
                lblNombre.setText(empleadoActual.getNombreCompleto());
                lblTipo.setText(empleadoActual.getEmpTipoJornada().name());
            } else {
                setEstado("⚠ No se encontró el empleado asociado a su cuenta.", Color.RED);
            }
        } catch (Exception ex) {
            setEstado("Error al cargar empleado: " + ex.getMessage(), Color.RED);
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(Estilos.BLANCO);

        // ── Cabecera ──────────────────────────────────────────
        JPanel pnlTop = new JPanel(new BorderLayout());
        pnlTop.setBackground(Estilos.ROJO_UTA);
        pnlTop.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        JPanel pnlTitles = new JPanel(new GridLayout(2, 1));
        pnlTitles.setOpaque(false);
        JLabel lblTit = new JLabel("Consultar Mis Asistencias");
        lblTit.setFont(Estilos.FUENTE_TITULO);
        lblTit.setForeground(Estilos.BLANCO);
        JLabel lblSub = new JLabel(
            "Ingrese su cédula y la fecha para ver sus horas registradas");
        lblSub.setFont(Estilos.FUENTE_PEQUENA);
        lblSub.setForeground(Estilos.BLANCO);
        pnlTitles.add(lblTit);
        pnlTitles.add(lblSub);
        pnlTop.add(pnlTitles, BorderLayout.WEST);

        // ── Nota sobre registro ───────────────────────────────
        JPanel pnlNota = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pnlNota.setBackground(new Color(255, 243, 205));
        pnlNota.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(255, 193, 7)));
        JLabel lblNota = new JLabel(
            "ℹ️  Para registrar su asistencia del día, use el botón verde " +
            "\"📋 REGISTRAR ASISTENCIA\" en la pantalla de inicio de sesión.");
        lblNota.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblNota.setForeground(Estilos.NEGRO_TEXTO);
        pnlNota.add(lblNota);

        // ── Panel de búsqueda ─────────────────────────────────
        JPanel pnlBuscar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        pnlBuscar.setBackground(Estilos.BLANCO);
        pnlBuscar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Estilos.GRIS_BORDE));

        pnlBuscar.add(etq("Mi Cédula (10 dígitos):"));
        txtCedula = new JTextField(12);
        // Solo acepta dígitos, máximo 10
        txtCedula.setDocument(new javax.swing.text.PlainDocument() {
            @Override public void insertString(int o, String s,
                    javax.swing.text.AttributeSet a) throws javax.swing.text.BadLocationException {
                if (s == null) return;
                String d = s.replaceAll("[^0-9]", "");
                if (getLength() + d.length() <= 10) super.insertString(o, d, a);
            }
        });
        Estilos.estilizarCampo(txtCedula);
        pnlBuscar.add(txtCedula);

        pnlBuscar.add(etq("Fecha (AAAA-MM-DD):"));
        txtFecha = new JTextField(LocalDate.now().toString(), 12);
        Estilos.estilizarCampo(txtFecha);
        pnlBuscar.add(txtFecha);

        JButton btnConsultar = new JButton("🔍 Consultar");
        JButton btnHoy       = new JButton("📅 Hoy");
        Estilos.estilizarBotonPrimario(btnConsultar);
        Estilos.estilizarBotonSecundario(btnHoy);
        pnlBuscar.add(btnConsultar);
        pnlBuscar.add(btnHoy);

        // ── Info empleado ─────────────────────────────────────
        JPanel pnlInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
        pnlInfo.setBackground(Estilos.BLANCO);

        lblNombre = new JLabel("—");
        lblNombre.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblNombre.setForeground(Estilos.NEGRO_TEXTO);

        lblTipo = new JLabel("");
        lblTipo.setFont(Estilos.FUENTE_NORMAL);
        lblTipo.setForeground(Estilos.NEGRO_TEXTO);

        pnlInfo.add(etq("Empleado:"));
        pnlInfo.add(lblNombre);
        pnlInfo.add(etq("  Jornada:"));
        pnlInfo.add(lblTipo);

        // ── Tabla de resultado ────────────────────────────────
        String[] cols = {"Fecha", "Entrada Mañana", "Salida Mañana",
                         "Entrada Tarde",  "Salida Tarde",
                         "Horas Trabajadas", "Atraso (min)", "Sal. Ant. (min)"};
        modelo = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tabla = new JTable(modelo);
        Estilos.estilizarTabla(tabla);
        tabla.setRowHeight(28);
        // Establecer color negro para los encabezados de las columnas
        tabla.getTableHeader().setForeground(Estilos.NEGRO_TEXTO);
        tabla.getTableHeader().setBackground(Estilos.BLANCO);
        tabla.getTableHeader().setReorderingAllowed(false);

        // ── Estado / mensaje ──────────────────────────────────
        lblEstado = new JLabel(
            "  Ingrese su cédula y la fecha para consultar su asistencia.",
            SwingConstants.LEFT);
        lblEstado.setFont(Estilos.FUENTE_NORMAL);
        lblEstado.setForeground(Estilos.NEGRO_TEXTO);
        lblEstado.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        // ── Ensamble ──────────────────────────────────────────
        JPanel pnlNorte = new JPanel(new BorderLayout());
        pnlNorte.add(pnlNota,   BorderLayout.NORTH);
        pnlNorte.add(pnlBuscar, BorderLayout.CENTER);
        pnlNorte.add(pnlInfo,   BorderLayout.SOUTH);

        add(pnlTop,               BorderLayout.NORTH);
        add(pnlNorte,             BorderLayout.CENTER);

        JPanel pnlMain = new JPanel(new BorderLayout());
        pnlMain.add(pnlNorte,              BorderLayout.NORTH);
        pnlMain.add(new JScrollPane(tabla), BorderLayout.CENTER);
        pnlMain.add(lblEstado,             BorderLayout.SOUTH);

        add(pnlTop,  BorderLayout.NORTH);
        add(pnlMain, BorderLayout.CENTER);

        // ── Eventos ──────────────────────────────────────────
        btnConsultar.addActionListener(e -> consultar());
        btnHoy.addActionListener(e -> {
            txtFecha.setText(LocalDate.now().toString());
            consultar();
        });
        txtFecha.addActionListener(e -> consultar());
        txtCedula.addActionListener(e -> consultar());
    }

    private void consultar() {
        String cedula = txtCedula.getText().trim();

        // Validar cédula
        String errCedula = Utilidades.mensajeValidacionCedula(cedula);
        if (errCedula != null) {
            setEstado("⚠ " + errCedula, Color.RED);
            return;
        }

        LocalDate fecha;
        try { fecha = LocalDate.parse(txtFecha.getText().trim()); }
        catch (Exception ex) {
            setEstado("⚠ Fecha inválida. Use el formato AAAA-MM-DD.", Color.RED);
            return;
        }

        try {
            Empleado emp = new EmpleadoDAO().buscarPorCedula(cedula);
            if (emp == null || !emp.isEmpActivo()) {
                setEstado("⚠ Empleado no encontrado o inactivo.", Color.RED);
                lblNombre.setText("—");
                lblTipo.setText("");
                modelo.setRowCount(0);
                return;
            }

            lblNombre.setText(emp.getNombreCompleto());
            lblTipo.setText(emp.getEmpTipoJornada().name());

            // Buscar asistencia de esa fecha
            Asistencia a = new AsistenciaDAO().obtenerDelDia(emp.getEmpId(), fecha);
            modelo.setRowCount(0);

            if (a == null) {
                setEstado("ℹ No hay registro de asistencia para el " +
                    fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".", Color.DARK_GRAY);
            } else {
                modelo.addRow(new Object[]{
                    a.getAsiFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    Utilidades.formatHora(a.getAsiEntradaManana()),
                    Utilidades.formatHora(a.getAsiSalidaManana()),
                    Utilidades.formatHora(a.getAsiEntradaTarde()),
                    Utilidades.formatHora(a.getAsiSalidaTarde()),
                    a.getAsiHorasTrabajadas() != null
                        ? a.getAsiHorasTrabajadas().toPlainString() + " h" : "0.00 h",
                    a.getAsiAtrasoMinutos() + " min",
                    a.getAsiSalidaAntMinutos() + " min"
                });

                // Mensaje de estado con resumen
                String estado = String.format(
                    "✔ Asistencia encontrada | Horas: %s | Atraso: %d min | " +
                    "Sal.Anticipada: %d min | Descuento: $%.2f",
                    a.getAsiHorasTrabajadas() != null
                        ? a.getAsiHorasTrabajadas().toPlainString() : "0.00",
                    a.getAsiAtrasoMinutos(),
                    a.getAsiSalidaAntMinutos(),
                    a.getAsiTotalDescMinutos() * 0.20);
                setEstado(estado, Estilos.VERDE_OK);
            }

        } catch (Exception ex) {
            setEstado("Error: " + ex.getMessage(), Color.RED);
            ex.printStackTrace();
        }
    }

    private JLabel etq(String t) {
        return Estilos.etiquetaFormulario(t);
    }

    private void setEstado(String msg, Color color) {
        lblEstado.setText("  " + msg);
        lblEstado.setForeground(color);
    }
}
