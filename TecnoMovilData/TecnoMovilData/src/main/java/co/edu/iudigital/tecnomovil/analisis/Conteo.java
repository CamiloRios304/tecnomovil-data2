package co.edu.iudigital.tecnomovil.analisis;

/** Par inmutable (clave, total) usado para rankings de estaciones, rutas y horas. */
public record Conteo<K>(K clave, long total) { }
