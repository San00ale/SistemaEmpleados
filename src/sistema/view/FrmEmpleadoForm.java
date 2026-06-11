package sistema.view;

import sistema.dao.EmpleadoDAO;
import sistema.dao.UsuarioDAO;
import sistema.model.Empleado;
import sistema.model.Usuario;
import sistema.util.Estilos;
import sistema.util.Utilidades;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

/**
 * Formulario alta/modificación de empleado.
 * Validación de cédula: exactamente 10 dígitos numéricos + verificador.
 */
public class FrmEmpleadoForm extends JPanel {

    private final Empleado empleado; // null = nuevo

    private JTextField     txtCedula, txtNombres, txtApellidos;
    private JTextField     txtCorreo, txtTelefono, txtCargo, txtFechaIngreso;
    private JComboBox<String> cboTipo;
    private JLabel         lblErrCedula;
    // Cuenta de acceso
    private JCheckBox      chkCrearUsuario;
    private JTextField     txtUsername;
    private JPasswordField txtPassUsuario;

    public FrmEmpleadoForm(Empleado emp) {
        this.empleado = emp;
        initComponents();
        if (emp != null) llenarFormulario(emp);
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(Estilos.BLANCO);

        // ── Cabecera ──
        JPanel pnlTop = new JPanel(new BorderLayout());
        pnlTop.setBackground(Estilos.ROJO_UTA);
        pnlTop.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        JLabel lbl = new JLabel(empleado == null ? "Registrar Nuevo Empleado" : "Modificar Empleado");
        lbl.setFont(Estilos.FUENTE_TITULO);
        lbl.setForeground(Estilos.BLANCO);
        pnlTop.add(lbl, BorderLayout.WEST);

        // ── Formulario ──
        JPanel pnlForm = new JPanel(new GridBagLayout());
        pnlForm.setBackground(Estilos.BLANCO);
        pnlForm.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        int f = 0;

        // Cédula (10 dígitos exactos)
        txtCedula = campo();
        txtCedula.setDocument(new javax.swing.text.PlainDocument() {
            @Override public void insertString(int offs, String str, javax.swing.text.AttributeSet a)
                    throws javax.swing.text.BadLocationException {
                if (str == null) return;
                // Solo dígitos y máximo 10 caracteres
                String soloDigitos = str.replaceAll("[^0-9]", "");
                if (getLength() + soloDigitos.length() <= 10)
                    super.insertString(offs, soloDigitos, a);
            }
        });
        agregarFila(pnlForm, gbc, f++, "Cédula * (10 dígitos):", txtCedula);

        // Label de error cédula
        lblErrCedula = new JLabel(" ");
        lblErrCedula.setFont(Estilos.FUENTE_PEQUENA);
        lblErrCedula.setForeground(Estilos.ROJO_ERROR);
        gbc.gridx = 1; gbc.gridy = f++;
        pnlForm.add(lblErrCedula, gbc);

        // Validar cédula al salir del campo
        txtCedula.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                String msg = Utilidades.mensajeValidacionCedula(txtCedula.getText().trim());
                lblErrCedula.setText(msg != null ? msg : "Cédula válida");
                lblErrCedula.setForeground(msg != null ? Estilos.ROJO_ERROR : Estilos.VERDE_OK);
            }
        });

        txtNombres   = campo(); agregarFila(pnlForm, gbc, f++, "Nombre *:", txtNombres);
        txtApellidos = campo(); agregarFila(pnlForm, gbc, f++, "Apellido *:", txtApellidos);
        txtCorreo    = campo(); agregarFila(pnlForm, gbc, f++, "Correo:", txtCorreo);
        txtTelefono  = campo();
        txtTelefono.setDocument(new javax.swing.text.PlainDocument() {
            @Override public void insertString(int offs, String str, javax.swing.text.AttributeSet a)
                    throws javax.swing.text.BadLocationException {
                if (str == null) return;
                // Solo dígitos y máximo 10 caracteres
                String soloDigitos = str.replaceAll("[^0-9]", "");
                if (getLength() + soloDigitos.length() <= 10)
                    super.insertString(offs, soloDigitos, a);
            }
        });
        agregarFila(pnlForm, gbc, f++, "Teléfono (máx 10 dígitos):", txtTelefono);
        txtCargo     = campo(); agregarFila(pnlForm, gbc, f++, "Cargo:", txtCargo);

        cboTipo = new JComboBox<>(new String[]{"COMPLETO", "PARCIAL"});
        Estilos.estilizarCombo(cboTipo);
        agregarFila(pnlForm, gbc, f++, "Tipo Jornada *:", cboTipo);

        txtFechaIngreso = campo();
        txtFechaIngreso.setText(LocalDate.now().toString());
        agregarFila(pnlForm, gbc, f++, "Fecha Ingreso (AAAA-MM-DD):", txtFechaIngreso);

        // Separador cuenta de acceso
        gbc.gridx = 0; gbc.gridy = f++; gbc.gridwidth = 2;
        pnlForm.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;

        chkCrearUsuario = new JCheckBox("Crear cuenta de acceso al sistema");
        chkCrearUsuario.setFont(Estilos.FUENTE_NORMAL);
        chkCrearUsuario.setForeground(Estilos.NEGRO_TEXTO);
        chkCrearUsuario.setBackground(Estilos.BLANCO);
        gbc.gridx = 0; gbc.gridy = f++; gbc.gridwidth = 2;
        pnlForm.add(chkCrearUsuario, gbc);
        gbc.gridwidth = 1;

        txtUsername    = campo(); txtUsername.setEnabled(false);
        txtPassUsuario = new JPasswordField(); Estilos.estilizarCampo(txtPassUsuario);
        txtPassUsuario.setEnabled(false);
        agregarFila(pnlForm, gbc, f++, "Username:", txtUsername);
        agregarFila(pnlForm, gbc, f++, "Contraseña:", txtPassUsuario);

        chkCrearUsuario.addActionListener(e -> {
            boolean sel = chkCrearUsuario.isSelected();
            txtUsername.setEnabled(sel);
            txtPassUsuario.setEnabled(sel);
        });

        if (empleado != null) {
            chkCrearUsuario.setVisible(false);
            txtUsername.setVisible(false);
            txtPassUsuario.setVisible(false);
        }

        // ── Botones ──
        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        pnlBtns.setBackground(Estilos.BLANCO);

        JButton btnGuardar  = new JButton("Guardar");
        JButton btnCancelar = new JButton("Cancelar");
        Estilos.estilizarBotonPrimario(btnGuardar);
        Estilos.estilizarBotonSecundario(btnCancelar);
        btnGuardar.setPreferredSize(new Dimension(140, 36));
        btnCancelar.setPreferredSize(new Dimension(110, 36));
        pnlBtns.add(btnGuardar);
        pnlBtns.add(btnCancelar);

        add(pnlTop,                    BorderLayout.NORTH);
        add(new JScrollPane(pnlForm),  BorderLayout.CENTER);
        add(pnlBtns,                   BorderLayout.SOUTH);

        btnGuardar.addActionListener(e -> guardar());
        btnCancelar.addActionListener(e -> cerrar());
    }

    private JTextField campo() {
        JTextField tf = new JTextField(22);
        Estilos.estilizarCampo(tf);
        return tf;
    }

    private void agregarFila(JPanel p, GridBagConstraints gbc, int fila,
                              String etiqueta, Component comp) {
        JLabel l = Estilos.etiquetaFormulario(etiqueta);
        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0.35;
        p.add(l, gbc);
        gbc.gridx = 1; gbc.weightx = 0.65;
        p.add(comp, gbc);
    }

    private void llenarFormulario(Empleado e) {
        txtCedula.setText(e.getEmpCedula());
        txtNombres.setText(e.getEmpNombres());
        txtApellidos.setText(e.getEmpApellidos());
        txtCorreo.setText(nvl(e.getEmpCorreo()));
        txtTelefono.setText(nvl(e.getEmpTelefono()));
        txtCargo.setText(nvl(e.getEmpCargo()));
        cboTipo.setSelectedItem(e.getEmpTipoJornada().name());
        txtFechaIngreso.setText(e.getEmpFechaIngreso() != null
                ? e.getEmpFechaIngreso().toString() : "");
    }

    private void guardar() {
        String cedula    = txtCedula.getText().trim();
        String nombres   = txtNombres.getText().trim();
        String apellidos = txtApellidos.getText().trim();

        // Validar cédula — exactamente 10 dígitos numéricos
        String errCedula = Utilidades.mensajeValidacionCedula(cedula);
        if (errCedula != null) { alerta(errCedula); return; }
        if (nombres.isEmpty() || apellidos.isEmpty()) {
            alerta("Nombres y apellidos son obligatorios."); return;
        }

        LocalDate fi;
        try { fi = LocalDate.parse(txtFechaIngreso.getText().trim()); }
        catch (Exception ex) { alerta("Fecha de ingreso inválida. Use AAAA-MM-DD."); return; }

        try {
            EmpleadoDAO dao = new EmpleadoDAO();
            // En inserción: verificar cédula duplicada
            if (empleado == null && dao.existeCedula(cedula)) {
                alerta("Ya existe un empleado con la cédula " + cedula + "."); return;
            }

            Empleado e = empleado != null ? empleado : new Empleado();
            e.setEmpCedula(cedula);
            e.setEmpNombres(nombres);
            e.setEmpApellidos(apellidos);
            e.setEmpCorreo(txtCorreo.getText().trim());
            e.setEmpTelefono(txtTelefono.getText().trim());
            e.setEmpCargo(txtCargo.getText().trim());
            e.setEmpTipoJornada(Empleado.TipoJornada.valueOf((String) cboTipo.getSelectedItem()));
            e.setEmpFechaIngreso(fi);
            e.setEmpActivo(true);

            // Crear cuenta de acceso si se solicitó
            if (chkCrearUsuario.isSelected() && empleado == null) {
                String uname = txtUsername.getText().trim();
                String upass = new String(txtPassUsuario.getPassword());
                if (uname.isEmpty() || upass.isEmpty()) {
                    alerta("Ingrese username y contraseña para la cuenta."); return;
                }
                UsuarioDAO uDao = new UsuarioDAO();
                if (uDao.existeUsername(uname)) {
                    alerta("El username '" + uname + "' ya existe."); return;
                }
                Usuario u = new Usuario();
                u.setUsuUsername(uname);
                u.setUsuPassword(sistema.util.Utilidades.sha256(upass));
                u.setUsuPerfil(Usuario.Perfil.EMPLEADO);
                int idU = uDao.insertar(u);
                e.setEmpIdUsuario(idU);
            }

            if (empleado == null) {
                dao.insertar(e);
                JOptionPane.showMessageDialog(this, "Empleado registrado correctamente.",
                        "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } else {
                dao.actualizar(e);
                JOptionPane.showMessageDialog(this, "Empleado actualizado correctamente.",
                        "Éxito", JOptionPane.INFORMATION_MESSAGE);
            }
            cerrar();
        } catch (Exception ex) {
            alerta("Error al guardar: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void alerta(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Aviso", JOptionPane.WARNING_MESSAGE);
    }

    private void cerrar() {
        // Cerrar solo el diálogo padre, no la ventana principal
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w != null && w instanceof JDialog) {
            w.dispose();
        }
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
