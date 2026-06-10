package sistema.view;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.view.JasperViewer;
import sistema.dao.AsistenciaDAO;
import sistema.dao.EmpleadoDAO;
import sistema.model.Asistencia;
import sistema.model.Empleado;
import sistema.util.Estilos;
import sistema.util.Utilidades;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.table.DefaultTableModel;

/**
 * Panel de reporte mensual por empleado — solo ADMIN.
 *
 * COMPLETO: muestra horas entrada/salida, atraso, salida anticipada,
 *           total descuento ($0.20/min) y sueldo neto sobre $1500.
 * PARCIAL : muestra horas trabajadas y pago ($5/hora, máx 8h/día).
 */
public class FrmReporteEmpleado extends JPanel {

    private JTextField        txtCedula;
    private JComboBox<String> cboMes, cboAnio;
    private JTable            tabla;
    private DefaultTableModel modelo;
    private JLabel            lblResumen;

    public FrmReporteEmpleado() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(Estilos.BLANCO);

        // ── Cabecera ──
        JPanel pnlTop = new JPanel(new BorderLayout());
        pnlTop.setBackground(Estilos.ROJO_UTA);
        pnlTop.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        JLabel lbl = new JLabel("Reporte Mensual por Empleado");
        lbl.setFont(Estilos.FUENTE_TITULO);
        lbl.setForeground(Estilos.BLANCO);
        pnlTop.add(lbl, BorderLayout.WEST);

        // ── Filtros ──
        JPanel pnlFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        pnlFiltros.setBackground(Estilos.BLANCO);
        pnlFiltros.setBorder(BorderFactory.createMatteBorder(0,0,1,0,Estilos.GRIS_BORDE));

        pnlFiltros.add(etq("Cédula:"));
        txtCedula = new JTextField(13);
        // Solo dígitos, máx 10
        txtCedula.setDocument(new javax.swing.text.PlainDocument() {
            @Override public void insertString(int o, String s, javax.swing.text.AttributeSet a)
                    throws javax.swing.text.BadLocationException {
                if (s == null) return;
                String d = s.replaceAll("[^0-9]", "");
                if (getLength() + d.length() <= 10) super.insertString(o, d, a);
            }
        });
        Estilos.estilizarCampo(txtCedula);
        pnlFiltros.add(txtCedula);

        pnlFiltros.add(etq("Mes:"));
        String[] meses = {"Enero","Febrero","Marzo","Abril","Mayo","Junio",
                          "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"};
        cboMes = new JComboBox<>(meses);
        Estilos.estilizarCombo(cboMes);
        cboMes.setSelectedIndex(LocalDate.now().getMonthValue() - 1);
        pnlFiltros.add(cboMes);

        pnlFiltros.add(etq("Año:"));
        int anioActual = LocalDate.now().getYear();
        String[] anios = new String[5];
        for (int i = 0; i < 5; i++) anios[i] = String.valueOf(anioActual - i);
        cboAnio = new JComboBox<>(anios);
        Estilos.estilizarCombo(cboAnio);
        pnlFiltros.add(cboAnio);

        JButton btnConsultar = new JButton("🔍 Consultar");
        JButton btnPDF       = new JButton("📄 Ver PDF");
        Estilos.estilizarBotonPrimario(btnConsultar);
        Estilos.estilizarBotonSecundario(btnPDF);
        pnlFiltros.add(btnConsultar);
        pnlFiltros.add(btnPDF);

        // ── Tabla — columnas varían según tipo ──
        String[] cols = {"Fecha","E.Mañana","S.Mañana","E.Tarde","S.Tarde",
                         "Atraso(min)","Sal.Ant(min)","Total Desc(min)",
                         "Descuento($)","Horas","Sueldo/Pago($)"};
        modelo = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tabla = new JTable(modelo);
        estilizarTabla(tabla);
        // Establecer color negro para los encabezados de las columnas
        tabla.getTableHeader().setForeground(Estilos.NEGRO_TEXTO);
        tabla.getTableHeader().setBackground(Estilos.BLANCO);
        tabla.getTableHeader().setReorderingAllowed(false);

        // ── Resumen ──
        lblResumen = new JLabel("  Ingrese cédula, mes y año, luego presione Consultar.");
        lblResumen.setFont(Estilos.FUENTE_NORMAL);
        lblResumen.setForeground(Estilos.NEGRO_TEXTO);
        lblResumen.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JPanel pnlMain = new JPanel(new BorderLayout());
        pnlMain.add(pnlFiltros,              BorderLayout.NORTH);
        pnlMain.add(new JScrollPane(tabla),  BorderLayout.CENTER);
        pnlMain.add(lblResumen,              BorderLayout.SOUTH);

        add(pnlTop,  BorderLayout.NORTH);
        add(pnlMain, BorderLayout.CENTER);

        btnConsultar.addActionListener(e -> consultar());
        btnPDF.addActionListener(e -> verPDF());
        txtCedula.addActionListener(e -> consultar());
    }

    private void consultar() {
        String cedula = txtCedula.getText().trim();
        if (cedula.length() != 10) { alerta("Ingrese una cédula de 10 dígitos."); return; }
        int mes  = cboMes.getSelectedIndex() + 1;
        int anio = Integer.parseInt((String) cboAnio.getSelectedItem());

        try {
            Empleado emp = new EmpleadoDAO().buscarPorCedula(cedula);
            if (emp == null) { alerta("Empleado no encontrado."); return; }

            List<Asistencia> lista =
                new AsistenciaDAO().reporteMensual(emp.getEmpId(), anio, mes);
            modelo.setRowCount(0);

            boolean esCompleto = emp.getEmpTipoJornada() == Empleado.TipoJornada.COMPLETO;
            double totalAtrasoMin = 0, totalSalAntMin = 0, totalDescMin = 0;
            double totalDescUSD = 0, totalHoras = 0, totalPago = 0;

            for (Asistencia a : lista) {
                totalAtrasoMin  += a.getAsiAtrasoMinutos();
                totalSalAntMin  += a.getAsiSalidaAntMinutos();
                totalDescMin    += a.getAsiTotalDescMinutos();
                totalDescUSD    += a.getAsiTotalDescMinutos() * 0.20;
                totalHoras      += a.getAsiHorasTrabajadas() != null
                                    ? a.getAsiHorasTrabajadas().doubleValue() : 0;

                double pagoDia = esCompleto
                    ? (1500.0 / 22.0) - (a.getAsiTotalDescMinutos() * 0.20)
                    : (a.getAsiHorasTrabajadas() != null
                        ? Math.min(a.getAsiHorasTrabajadas().doubleValue(), 8.0) * 5.0 : 0);
                totalPago += pagoDia;

                modelo.addRow(new Object[]{
                    a.getAsiFecha().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    Utilidades.formatHora(a.getAsiEntradaManana()),
                    Utilidades.formatHora(a.getAsiSalidaManana()),
                    Utilidades.formatHora(a.getAsiEntradaTarde()),
                    Utilidades.formatHora(a.getAsiSalidaTarde()),
                    a.getAsiAtrasoMinutos(),
                    a.getAsiSalidaAntMinutos(),
                    a.getAsiTotalDescMinutos(),
                    String.format("$%.2f", a.getAsiTotalDescMinutos() * 0.20),
                    a.getAsiHorasTrabajadas() != null
                        ? a.getAsiHorasTrabajadas().toPlainString() : "0.00",
                    String.format("$%.2f", pagoDia)
                });
            }

            String resumen;
            if (esCompleto) {
                resumen = String.format(
                    "  %s [COMPLETO] | Días: %d | Atraso: %d min | Sal.Ant: %d min | " +
                    "Total desc: $%.2f | Sueldo neto mes: $%.2f",
                    emp.getNombreCompleto(), lista.size(),
                    (int)totalAtrasoMin, (int)totalSalAntMin,
                    totalDescUSD, 1500.0 - totalDescUSD);
            } else {
                resumen = String.format(
                    "  %s [PARCIAL] | Días: %d | Total horas: %.2f | Total a cobrar: $%.2f",
                    emp.getNombreCompleto(), lista.size(), totalHoras, totalPago);
            }
            lblResumen.setText(resumen);

        } catch (Exception ex) {
            alerta("Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void verPDF() {
        String cedula = txtCedula.getText().trim();
        if (cedula.length() != 10) { alerta("Ingrese una cédula de 10 dígitos."); return; }
        int mes  = cboMes.getSelectedIndex() + 1;
        int anio = Integer.parseInt((String) cboAnio.getSelectedItem());
        try {
            Empleado emp = new EmpleadoDAO().buscarPorCedula(cedula);
            if (emp == null) { alerta("Empleado no encontrado."); return; }
            List<Asistencia> lista =
                new AsistenciaDAO().reporteMensual(emp.getEmpId(), anio, mes);
            if (lista.isEmpty()) { alerta("Sin registros para ese período."); return; }
            lista.forEach(a -> a.setTipoJornada(emp.getEmpTipoJornada().name()));

            boolean esCompleto = emp.getEmpTipoJornada() == Empleado.TipoJornada.COMPLETO;
            String jrxml = esCompleto
                    ? "/sistema/reports/ReporteCompleto.jrxml"
                    : "/sistema/reports/ReporteParcial.jrxml";

            try (InputStream is = getClass().getResourceAsStream(jrxml)) {
            if (is == null) { alerta("Archivo de reporte no encontrado:\n" + jrxml); return; }

            JasperReport jr = JasperCompileManager.compileReport(is);
            Map<String, Object> params = new HashMap<>();
            params.put("NOMBRE_EMPLEADO", emp.getNombreCompleto());
            params.put("CEDULA",          emp.getEmpCedula());
            params.put("MES_ANIO",        meses()[mes-1] + " " + anio);
            params.put("TIPO_JORNADA",    emp.getEmpTipoJornada().name());
            JasperPrint print = JasperFillManager.fillReport(jr, params,
                    new JRBeanCollectionDataSource(lista));
            JasperViewer.viewReport(print, false);
            }
        } catch (Exception ex) {
            alerta("Error al generar PDF: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private String[] meses() {
        return new String[]{"Enero","Febrero","Marzo","Abril","Mayo","Junio",
                            "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"};
    }

    private void estilizarTabla(JTable t) {
        Estilos.estilizarTabla(t);
    }

    private JLabel etq(String t) {
        return Estilos.etiquetaFormulario(t);
    }

    private void alerta(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Aviso", JOptionPane.WARNING_MESSAGE);
    }
}
