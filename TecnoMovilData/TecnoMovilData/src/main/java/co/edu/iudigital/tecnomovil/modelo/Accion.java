package co.edu.iudigital.tecnomovil.modelo;

import java.util.Locale;

/**
 * Acción registrada por un validador o sensor del sistema.
 * "entrada": el usuario ingresa a la estación.
 * "salida": el usuario valida para salir o cambia de ruta.
 */
public enum Accion {
    ENTRADA, SALIDA;

    /** Convierte el texto original ("entrada" o "salida") en el valor del enum. */
    public static Accion desdeTexto(String texto) {
        return switch (texto.toLowerCase(Locale.ROOT)) {
            case "entrada" -> ENTRADA;
            case "salida" -> SALIDA;
            default -> throw new IllegalArgumentException("Acción desconocida: " + texto);
        };
    }

    /** Representación textual tal como aparece en los registros originales. */
    public String texto() {
        return name().toLowerCase(Locale.ROOT);
    }
}
