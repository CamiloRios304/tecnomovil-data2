package co.edu.iudigital.tecnomovil.analisis;

/** Tiempo promedio (en minutos) que tardan los usuarios en ir de una estación a otra. */
public record TiempoTramo(String origen, String destino, double minutosPromedio, long muestras) { }
