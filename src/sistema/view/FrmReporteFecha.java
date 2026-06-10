package sistema.view;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.view.JasperViewer;
import sistema.dao.AsistenciaDAO;
import sistema.model.Asistencia;
import sistema.util.Estilos;
import sistema.util.Utilidades;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.table.DefaultTableModel;

/**
 * Panel de asistencias por fecha — solo ADMIN.
 * Muestra todos los empleados presentes ese día con horas E/S.
 */
public class FrmReporteFecha extends JPanel {

    private JTextField        txtFecha;
    private JTable            tabla;
    private DefaultTableModel modelo;
    private JLabel            lblTotal;

    public FrmReporteFecha() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(Estilos.BLANCO);

        // ── Cabecera ──
        JPanel pnlTop = new JPanel(new BorderLayout());
        pnlTop.setBackground(Estilos.ROJO_UTA);
        pnlTop.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        JLabel lbl = new JLabel("Reporte de Asistencias por Fecha");
        lbl.setFont(Estilos.FUENTE_TITULO);
        lbl.setForeground(Estilos.BLANCO);
        pnlTop.add(lbl, BorderLayout.WEST);

        // ── Filtros ──
        JPanel pnlFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        pnlFiltros.setBackground(Estilos.BLANCO);
        pnlFiltros.setBorder(BorderFactory.createMatteBorder(0,0,1,0,Estilos.GRIS_BORDE));

        pnlFiltros.add(etq("Fecha (AAAA-MM-DD):"));
        txtFecha = new JTextField(LocalDate.now().toString(), 12);
        Estilos.estilizarCampo(txtFecha);
        pnlFiltros.add(txtFecha);

        JButton btnConsultar = new JButton("🔍 Consultar");
        JButton btnHoy       = new JButton("📅 Hoy");
        JButton btnPDF       = new JButton("📄 Ver PDF");
        Estilos.estilizarBotonPrimario(btnConsultar);
        Estilos.estilizarBotonSecundario(btnHoy);
        Estilos.estilizarBotonSecundario(btnPDF);
        pnlFiltros.add(btnConsultar);
        pnlFiltros.add(btnHoy);
        pnlFiltros.add(btnPDF);

        // ── Tabla ──
        String[] cols = {"Cédula","Nombre","Tipo","E.Mañana","S.Mañana",
                         "E.Tarde","S.Tarde","Horas","Atraso(min)","Sal.Ant(min)"};
        modelo = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tabla = new JTable(modelo);
        Estilos.estilizarTabla(tabla);
        // Establecer color negro para los encabezados de las columnas
        tabla.getTableHeader().setForeground(Estilos.NEGRO_TEXTO);
        tabla.getTableHeader().setBackground(Estilos.BLANCO);
        tabla.getTableHeader().setReorderingAllowed(false);

        lblTotal = new JLabel("  Seleccione fecha y presione Consultar.");
        lblTotal.setFont(Estilos.FUENTE_NORMAL);
        lblTotal.setForeground(Estilos.NEGRO_TEXTO);
        lblTotal.setBorder(BorderFactory.createEmptyBorder(6,10,6,10));

        JPanel pnlMain = new JPanel(new BorderLayout());
        pnlMain.add(pnlFiltros,              BorderLayout.NORTH);
        pnlMain.add(new JScrollPane(tabla),  BorderLayout.CENTER);
        pnlMain.add(lblTotal,                BorderLayout.SOUTH);

        add(pnlTop,  BorderLayout.NORTH);
        add(pnlMain, BorderLayout.CENTER);

        btnConsultar.addActionListener(e -> consultar());
        btnHoy.addActionListener(e -> { txtFecha.setText(LocalDate.now().toString()); consultar(); });
        btnPDF.addActionListener(e -> verPDF());
        txtFecha.addActionListener(e -> consultar());
    }

    private void consultar() {
        LocalDate fecha;
        try { fecha = LocalDate.parse(txtFecha.getText().trim()); }
        catch (Exception ex) { alerta("Fecha inválida. Use AAAA-MM-DD."); return; }

        try {
            List<Asistencia> lista = new AsistenciaDAO().porFecha(fecha);
            modelo.setRowCount(0);
            int totalEmp = lista.size(), totalAtraso = 0;
            double totalHoras = 0;

            for (Asistencia a : lista) {
                totalAtraso += a.getAsiAtrasoMinutos();
                totalHoras  += a.getAsiHorasTrabajadas() != null
                                ? a.getAsiHorasTrabajadas().doubleValue() : 0;
                modelo.addRow(new Object[]{
                    a.getCedulaEmpleado(),
                    a.getNombreEmpleado(),
                    a.getTipoJornada(),
                    Utilidades.formatHora(a.getAsiEntradaManana()),
                    Utilidades.formatHora(a.getAsiSalidaManana()),
                    Utilidades.formatHora(a.getAsiEntradaTarde()),
                    Utilidades.formatHora(a.getAsiSalidaTarde()),
                    a.getAsiHorasTrabajadas() != null
                        ? a.getAsiHorasTrabajadas().toPlainString() : "0.00",
                    a.getAsiAtrasoMinutos(),
                    a.getAsiSalidaAntMinutos()
                });
            }

            lblTotal.setText(String.format(
                "  Fecha: %s | Empleados presentes: %d | " +
                "Total horas: %.2f | Total atraso: %d min",
                fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                totalEmp, totalHoras, totalAtraso));

            if (lista.isEmpty())
                lblTotal.setText("  Sin registros para el " +
                    fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        } catch (Exception ex) {
            alerta("Error: " + ex.getMessage());
        }
    }

    private void verPDF() {
        LocalDate fecha;
        try { fecha = LocalDate.parse(txtFecha.getText().trim()); }
        catch (Exception ex) { alerta("Fecha inválida."); return; }
        try {
            List<Asistencia> lista = new AsistenciaDAO().porFecha(fecha);
            if (lista.isEmpty()) { alerta("Sin registros para esa fecha."); return; }
            try (InputStream is = getClass().getResourceAsStream("/sistema/reports/ReporteFecha.jrxml")) {
                if (is == null) { alerta("Reporte no encontrado."); return; }
                JasperReport jr = JasperCompileManager.compileReport(is);
                Map<String, Object> p = new HashMap<>();
                p.put("FECHA_REPORTE", fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                JasperPrint print = JasperFillManager.fillReport(jr, p,
                        new JRBeanCollectionDataSource(lista));
                JasperViewer.viewReport(print, false);
            }
        } catch (Exception ex) {
            alerta("Error PDF: " + ex.getMessage());
        }
    }

    private JLabel etq(String t) {
        return Estilos.etiquetaFormulario(t);
    }
    private void alerta(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Aviso", JOptionPane.WARNING_MESSAGE);
    }
}
