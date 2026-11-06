package co.edu.iudigital.tecnomovil.analisis;

/**
 * Estado de ocupación de una ruta en su hora más cargada.
 * ocupacion = pasajerosHoraPico / capacidadHora; es crítica si supera el umbral.
 */
public record EstadoRuta(String ruta,
                         long pasajerosHoraPico,
                         int capacidadHora,
                         double ocupacion,
                         boolean critica) { }
