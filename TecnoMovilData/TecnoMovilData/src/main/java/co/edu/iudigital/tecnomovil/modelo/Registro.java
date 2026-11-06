package co.edu.iudigital.tecnomovil.modelo;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Registro inmutable generado por validadores, sensores, apps o cámaras (sección 3.1 del caso).
 * Al ser un record, todos sus campos son finales y no existen métodos que los modifiquen.
 */
public record Registro(String idUsuario,
                       String ruta,
                       String estacion,
                       Accion accion,
                       LocalDateTime timestamp) {

    public Registro {
        Objects.requireNonNull(idUsuario, "idUsuario");
        Objects.requireNonNull(ruta, "ruta");
        Objects.requireNonNull(estacion, "estacion");
        Objects.requireNonNull(accion, "accion");
        Objects.requireNonNull(timestamp, "timestamp");
    }

    /** Hora del día (0-23) del registro. Función pura: depende solo del propio registro. */
    public int hora() {
        return timestamp.getHour();
    }
}
