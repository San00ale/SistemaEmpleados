package sistema.view;

import sistema.dao.EmpleadoDAO;
import sistema.model.Empleado;
import sistema.util.Estilos;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

/**
 * Panel de listado de empleados — solo ADMIN.
 * Muestra: cédula, nombre, cargo, tipo jornada, estado, foto.
 */
public class FrmEmpleados extends JPanel {

    private JTable            tabla;
    private DefaultTableModel modelo;
    private JTextField        txtBuscar;
    private List<Empleado>    empleados;

    public FrmEmpleados() {
        initComponents();
        cargarDatos();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(Estilos.BLANCO);

        // ── Cabecera ──
        JPanel pnlTop = new JPanel(new BorderLayout());
        pnlTop.setBackground(Estilos.ROJO_UTA);
        pnlTop.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        JLabel lbl = new JLabel("Gestión de Empleados");
        lbl.setFont(Estilos.FUENTE_TITULO);
        lbl.setForeground(Estilos.BLANCO);
        pnlTop.add(lbl, BorderLayout.WEST);

        JPanel pnlBtnsTop = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        pnlBtnsTop.setOpaque(false);
        txtBuscar = new JTextField(16);
        Estilos.estilizarCampo(txtBuscar);
        txtBuscar.setToolTipText("Buscar por nombre o cédula");
        JButton btnBuscar    = new JButton("buscar");
        JButton btnNuevo     = new JButton(" Nuevo");
        JButton btnRefrescar = new JButton("refrescar");
        Estilos.estilizarBotonSecundario(btnBuscar);
        Estilos.estilizarBotonPrimario(btnNuevo);
        Estilos.estilizarBotonSecundario(btnRefrescar);
        btnBuscar.setBackground(Estilos.BLANCO);
        btnRefrescar.setBackground(Estilos.BLANCO);
        pnlBtnsTop.add(txtBuscar);
        pnlBtnsTop.add(btnBuscar);
        pnlBtnsTop.add(btnNuevo);
        pnlBtnsTop.add(btnRefrescar);
        pnlTop.add(pnlBtnsTop, BorderLayout.EAST);

        // ── Tabla ──
        String[] cols = {"ID", "Cédula", "Nombres", "Apellidos",
                         "Cargo", "Tipo Jornada", "Estado", " Foto"};
        modelo = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tabla = new JTable(modelo);
        Estilos.estilizarTabla(tabla);
        tabla.setRowHeight(26);
        tabla.getColumnModel().getColumn(0).setMinWidth(0);
        tabla.getColumnModel().getColumn(0).setMaxWidth(0); // ocultar ID
        // Establecer color negro para los encabezados de las columnas
        tabla.getTableHeader().setForeground(Estilos.NEGRO_TEXTO);
        tabla.getTableHeader().setBackground(Estilos.BLANCO);
        tabla.getTableHeader().setReorderingAllowed(false);

        // ── Botones acción ──
        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        pnlBtns.setBackground(Estilos.BLANCO);
        JButton btnEditar = new JButton(" Editar");
        JButton btnElim   = new JButton(" Eliminar");
        Estilos.estilizarBotonPrimario(btnEditar);
        Estilos.estilizarBotonSecundario(btnElim);
        btnElim.setForeground(Estilos.ROJO_ERROR);
        btnElim.setBorder(BorderFactory.createLineBorder(Estilos.ROJO_ERROR));
        pnlBtns.add(btnEditar);
        pnlBtns.add(btnElim);

        add(pnlTop,               BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(pnlBtns,              BorderLayout.SOUTH);

        // ── Eventos ──
        btnNuevo.addActionListener(e -> abrirFormulario(null));
        btnEditar.addActionListener(e -> editarSeleccionado());
        btnElim.addActionListener(e -> eliminarSeleccionado());
        btnBuscar.addActionListener(e -> filtrar(txtBuscar.getText().trim()));
        txtBuscar.addActionListener(e -> filtrar(txtBuscar.getText().trim()));
        btnRefrescar.addActionListener(e -> cargarDatos());
    }

    private void cargarDatos() {
        try {
            empleados = new EmpleadoDAO().listarTodos();
            poblarTabla(empleados);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar empleados: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void poblarTabla(List<Empleado> lista) {
        modelo.setRowCount(0);
        for (Empleado e : lista) {
            modelo.addRow(new Object[]{
                e.getEmpId(),
                e.getEmpCedula(),
                e.getEmpNombres(),
                e.getEmpApellidos(),
                e.getEmpCargo(),
                e.getEmpTipoJornada().name(),
                e.isEmpActivo() ? "Activo" : "Inactivo",
                e.isEmpTieneFoto() ? "Sí" : "No"
            });
        }
    }

    private void filtrar(String texto) {
        if (texto.isEmpty()) { poblarTabla(empleados); return; }
        String t = texto.toLowerCase();
        poblarTabla(empleados.stream()
            .filter(e -> e.getEmpCedula().contains(t)
                      || e.getEmpNombres().toLowerCase().contains(t)
                      || e.getEmpApellidos().toLowerCase().contains(t))
            .toList());
    }

    private void abrirFormulario(Empleado emp) {
        FrmEmpleadoForm form = new FrmEmpleadoForm(emp);
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                emp == null ? "Nuevo Empleado" : "Editar Empleado", true);
        dlg.setContentPane(form);
        dlg.setSize(540, 560);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
        cargarDatos();
    }

    private void editarSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) { JOptionPane.showMessageDialog(this, "Seleccione un empleado."); return; }
        int id = (int) modelo.getValueAt(fila, 0);
        empleados.stream().filter(e -> e.getEmpId() == id)
                 .findFirst().ifPresent(this::abrirFormulario);
    }

    private void eliminarSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) { JOptionPane.showMessageDialog(this, "Seleccione un empleado."); return; }
        int id = (int) modelo.getValueAt(fila, 0);
        String nombre = modelo.getValueAt(fila, 2) + " " + modelo.getValueAt(fila, 3);
        int op = JOptionPane.showConfirmDialog(this,
            "¿Eliminar (baja lógica) a:\n" + nombre + "?",
            "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (op == JOptionPane.YES_OPTION) {
            try {
                new EmpleadoDAO().eliminar(id);
                JOptionPane.showMessageDialog(this, "Empleado eliminado.");
                cargarDatos();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
