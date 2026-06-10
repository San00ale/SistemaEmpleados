package sistema.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Métodos utilitarios del sistema. */
public final class Utilidades {

    public static final DateTimeFormatter FMT_HORA  = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Utilidades() {}

    /** SHA-256 en hexadecimal minúsculas. */
    public static String sha256(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(texto.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error SHA-256", e);
        }
    }

    /** Formatea hora o "--" si es null. */
    public static String formatHora(LocalTime t) {
        return t != null ? t.format(FMT_HORA) : "--:--";
    }

    /**
     * Valida cédula ecuatoriana:
     *  - Exactamente 10 caracteres
     *  - Solo dígitos numéricos
     *  - Dígito verificador válido (algoritmo módulo 10)
     */
    public static boolean validarCedula(String cedula) {
        if (cedula == null) return false;
        // Exactamente 10 dígitos numéricos
        if (!cedula.matches("^[0-9]{10}$")) return false;
        // Algoritmo verificador
        int[] coef = {2,1,2,1,2,1,2,1,2};
        int suma = 0;
        for (int i = 0; i < 9; i++) {
            int val = Character.getNumericValue(cedula.charAt(i)) * coef[i];
            if (val >= 10) val -= 9;
            suma += val;
        }
        int verificador = Character.getNumericValue(cedula.charAt(9));
        int residuo = suma % 10;
        return (residuo == 0 ? 0 : 10 - residuo) == verificador;
    }

    /**
     * Valida que la cédula tenga exactamente 10 dígitos numéricos.
     * Versión simplificada sin verificador (útil para mostrar error específico).
     */
    public static String mensajeValidacionCedula(String cedula) {
        if (cedula == null || cedula.trim().isEmpty())
            return "La cédula es obligatoria.";
        if (!cedula.matches("[0-9]+"))
            return "La cédula solo debe contener números.";
        if (cedula.length() < 10)
            return "La cédula debe tener 10 dígitos (faltan " + (10 - cedula.length()) + ").";
        if (cedula.length() > 10)
            return "La cédula debe tener exactamente 10 dígitos.";
        if (!validarCedula(cedula))
            return "La cédula ingresada no es válida (dígito verificador incorrecto).";
        return null; // null = válida
    }
}
