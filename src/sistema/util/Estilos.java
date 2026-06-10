package sistema.util;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * Constantes de estilo — colores institucionales UTA (rojo y blanco).
 */
public final class Estilos {

    // ── Paleta UTA ─────────────────────────────────────────────
    public static final Color ROJO_UTA       = new Color(180, 0,   0);
    public static final Color ROJO_OSCURO    = new Color(120, 0,   0);
    public static final Color ROJO_CLARO     = new Color(220, 50, 50);
    public static final Color BLANCO         = Color.WHITE;
    public static final Color GRIS_FONDO     = new Color(245, 245, 245);
    public static final Color GRIS_BORDE     = new Color(200, 200, 200);
    /** Texto legible en formularios, tablas y campos de entrada. */
    public static final Color NEGRO_TEXTO    = Color.BLACK;
    public static final Color ROJO_ERROR     = new Color(200, 0,   0);
    public static final Color VERDE_OK       = new Color(0,  140,  0);

    // ── Fuentes ────────────────────────────────────────────────
    public static final Font FUENTE_TITULO   = new Font("Segoe UI", Font.BOLD,  18);
    public static final Font FUENTE_SUBTIT   = new Font("Segoe UI", Font.BOLD,  13);
    public static final Font FUENTE_NORMAL   = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FUENTE_PEQUENA  = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FUENTE_BOTON    = new Font("Segoe UI", Font.BOLD,  12);

    private Estilos() {}

    /**
     * Tema global: todo lo que el usuario escribe o lee en campos de entrada
     * se muestra en negro sobre fondo blanco.
     * Llamar una vez al iniciar la aplicación (antes de crear ventanas).
     */
    public static void aplicarTemaEntradaTextoNegro() {
        Color negro = NEGRO_TEXTO;
        Color blanco = BLANCO;
        UIManager.put("TextField.foreground", negro);
        UIManager.put("TextField.background", blanco);
        UIManager.put("TextField.caretForeground", negro);
        UIManager.put("TextField.inactiveForeground", negro);
        UIManager.put("PasswordField.foreground", negro);
        UIManager.put("PasswordField.background", blanco);
        UIManager.put("PasswordField.caretForeground", negro);
        UIManager.put("FormattedTextField.foreground", negro);
        UIManager.put("FormattedTextField.background", blanco);
        UIManager.put("TextArea.foreground", negro);
        UIManager.put("TextArea.background", blanco);
        UIManager.put("TextArea.caretForeground", negro);
        UIManager.put("TextPane.foreground", negro);
        UIManager.put("EditorPane.foreground", negro);
        UIManager.put("ComboBox.foreground", negro);
        UIManager.put("ComboBox.background", blanco);
        UIManager.put("ComboBox.selectionForeground", negro);
        UIManager.put("ComboBox.selectionBackground", new Color(255, 220, 220));
        UIManager.put("Table.foreground", negro);
        UIManager.put("Table.background", blanco);
        UIManager.put("Table.selectionForeground", negro);
        UIManager.put("Table.selectionBackground", new Color(255, 220, 220));
        UIManager.put("TableHeader.foreground", negro);
        UIManager.put("TableHeader.background", blanco);
        UIManager.put("CheckBox.foreground", negro);
        UIManager.put("CheckBox.background", blanco);
        UIManager.put("OptionPane.messageForeground", negro);
        UIManager.put("Label.foreground", negro);
        UIManager.put("Label.background", blanco);
        UIManager.put("Button.foreground", negro);
        UIManager.put("Button.background", blanco);
        UIManager.put("Panel.background", blanco);
    }

    /** Etiqueta de formulario (texto negro). */
    public static JLabel etiquetaFormulario(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(FUENTE_NORMAL);
        l.setForeground(NEGRO_TEXTO);
        l.setBackground(BLANCO);
        l.setOpaque(true);
        return l;
    }

    /** Aplica estilo de botón principal (rojo/blanco). */
    public static void estilizarBotonPrimario(javax.swing.JButton btn) {
        btn.setBackground(ROJO_UTA);
        btn.setForeground(BLANCO);
        btn.setFont(FUENTE_BOTON);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    /** Aplica estilo de botón secundario (blanco/negro). */
    public static void estilizarBotonSecundario(javax.swing.JButton btn) {
        btn.setBackground(BLANCO);
        btn.setForeground(NEGRO_TEXTO);
        btn.setFont(FUENTE_BOTON);
        btn.setFocusPainted(false);
        btn.setBorder(javax.swing.BorderFactory.createLineBorder(ROJO_UTA, 1));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    /** Campo de texto o contraseña: texto negro, fondo blanco. */
    public static void estilizarCampo(javax.swing.JTextField tf) {
        tf.setFont(FUENTE_NORMAL);
        tf.setForeground(NEGRO_TEXTO);
        tf.setBackground(BLANCO);
        tf.setCaretColor(NEGRO_TEXTO);
        tf.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(GRIS_BORDE),
                javax.swing.BorderFactory.createEmptyBorder(4, 6, 4, 6)));
    }

    /** Lista desplegable: texto negro. */
    public static void estilizarCombo(JComboBox<?> cbo) {
        cbo.setFont(FUENTE_NORMAL);
        cbo.setForeground(NEGRO_TEXTO);
        cbo.setBackground(BLANCO);
    }

    /** Área de texto multilínea. */
    public static void estilizarAreaTexto(javax.swing.JTextArea area) {
        area.setFont(FUENTE_PEQUENA);
        area.setForeground(NEGRO_TEXTO);
        area.setBackground(BLANCO);
        area.setCaretColor(NEGRO_TEXTO);
    }

    /** Tabla de datos: celdas con texto negro. */
    public static void estilizarTabla(javax.swing.JTable tabla) {
        tabla.setFont(FUENTE_NORMAL);
        tabla.setForeground(NEGRO_TEXTO);
        tabla.setBackground(BLANCO);
        tabla.setRowHeight(24);
        tabla.setSelectionBackground(new Color(255, 220, 220));
        tabla.setSelectionForeground(NEGRO_TEXTO);
        tabla.setGridColor(GRIS_BORDE);
        JTableHeader header = tabla.getTableHeader();
        if (header != null) {
            header.setBackground(ROJO_UTA);
            header.setForeground(BLANCO);
            header.setFont(FUENTE_SUBTIT);
        }
    }
}
