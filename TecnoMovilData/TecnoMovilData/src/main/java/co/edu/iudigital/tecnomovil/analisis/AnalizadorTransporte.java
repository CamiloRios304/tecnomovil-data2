package co.edu.iudigital.tecnomovil.analisis;

import co.edu.iudigital.tecnomovil.modelo.Accion;
import co.edu.iudigital.tecnomovil.modelo.Registro;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Operaciones funcionales del módulo de procesamiento de TecnoMovil Data.
 *
 * Todas las funciones son puras: reciben un Stream (o valores) y devuelven colecciones
 * inmutables, sin modificar la fuente ni compartir estado. Quien llama decide si el
 * Stream es secuencial (lista.stream()) o paralelo (lista.parallelStream()).
 */
public final class AnalizadorTransporte {

    private AnalizadorTransporte() { }

    /** Predicado reutilizable: el registro es una entrada a estación. */
    public static final Predicate<Registro> ES_ENTRADA = r -> r.accion() == Accion.ENTRADA;

    // ------------------------------------------------------------------
    // Funciones de orden superior
    // ------------------------------------------------------------------

    /**
     * Función de orden superior: recibe un filtro y un clasificador (ambas son lambdas)
     * y devuelve el conteo de registros por clave, como Map inmutable.
     */
    public static <K> Map<K, Long> contarPor(Stream<Registro> registros,
                                             Predicate<Registro> filtro,
                                             Function<Registro, K> clasificador) {
        return registros
                .filter(filtro)
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(clasificador, Collectors.counting()),
                        Map::copyOf));
    }

    /** Función que devuelve otra función (closure): predicado "supera el umbral". */
    public static Predicate<Double> superaUmbral(double umbral) {
        return ocupacion -> ocupacion > umbral;
    }

    // ------------------------------------------------------------------
    // a) Afluencia por estación
    // ------------------------------------------------------------------

    /** Usuarios que ingresan a cada estación durante el día, de mayor a menor. */
    public static List<Conteo<String>> afluenciaPorEstacion(Stream<Registro> registros) {
        return aConteos(contarPor(registros, ES_ENTRADA, Registro::estacion), porTotalDescendente());
    }

    // ------------------------------------------------------------------
    // b) Horas pico
    // ------------------------------------------------------------------

    /** Distribución de ingresos por hora del día (0-23), ordenada por hora. */
    public static List<Conteo<Integer>> flujoPorHora(Stream<Registro> registros) {
        return aConteos(contarPor(registros, ES_ENTRADA, Registro::hora),
                Comparator.comparing((Conteo<Integer> c) -> c.clave()));
    }

    /** Las n horas de mayor flujo a partir de la distribución por hora. */
    public static List<Conteo<Integer>> horasPico(List<Conteo<Integer>> flujo, int n) {
        return flujo.stream()
                .sorted(AnalizadorTransporte.<Integer>porTotalDescendente())
                .limit(n)
                .toList();
    }

    // ------------------------------------------------------------------
    // c) Rutas más utilizadas
    // ------------------------------------------------------------------

    /** Las n rutas con mayor volumen de uso (ingresos). */
    public static List<Conteo<String>> rutasMasUtilizadas(Stream<Registro> registros, int n) {
        return aConteos(contarPor(registros, ES_ENTRADA, Registro::ruta), porTotalDescendente())
                .stream()
                .limit(n)
                .toList();
    }

    // ------------------------------------------------------------------
    // d) Patrones de viaje por usuario
    // ------------------------------------------------------------------

    /** Para cada usuario, estaciones visitadas en el orden cronológico de sus registros. */
    public static Map<String, List<String>> patronesDeViaje(Stream<Registro> registros) {
        return registros
                .sorted(Comparator.comparing(Registro::timestamp))
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(Registro::idUsuario,
                                Collectors.mapping(Registro::estacion, Collectors.toList())),
                        mapa -> mapa.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                                Map.Entry::getKey, e -> List.copyOf(e.getValue())))));
    }

    // ------------------------------------------------------------------
    // e) Tiempo promedio entre estaciones
    // ------------------------------------------------------------------

    private record ParEstaciones(String origen, String destino) { }

    private record Tramo(ParEstaciones par, long segundos) { }

    private record Consecutivos(Registro primero, Registro segundo) { }

    /** Tiempo promedio por par (origen, destino), de mayor a menor número de muestras. */
    public static List<TiempoTramo> tiempoPromedioEntreEstaciones(Stream<Registro> registros) {
        return tramos(registros)
                .collect(Collectors.groupingBy(Tramo::par, Collectors.summarizingLong(Tramo::segundos)))
                .entrySet().stream()
                .map(e -> new TiempoTramo(
                        e.getKey().origen(),
                        e.getKey().destino(),
                        e.getValue().getSum() / (double) e.getValue().getCount() / 60.0,
                        e.getValue().getCount()))
                .sorted(Comparator.comparingLong(TiempoTramo::muestras).reversed()
                        .thenComparing(TiempoTramo::origen)
                        .thenComparing(TiempoTramo::destino))
                .toList();
    }

    /** Tiempo promedio global (minutos) de todos los tramos entre estaciones. */
    public static double tiempoPromedioGlobal(Stream<Registro> registros) {
        return tramos(registros).mapToLong(Tramo::segundos).average().orElse(0.0) / 60.0;
    }

    /** Tramos (entrada -> salida en la misma ruta) entre registros consecutivos de cada usuario. */
    private static Stream<Tramo> tramos(Stream<Registro> registros) {
        return registros
                .sorted(Comparator.comparing(Registro::timestamp))
                .collect(Collectors.groupingBy(Registro::idUsuario))
                .values().stream()
                .flatMap(AnalizadorTransporte::tramosDeUsuario);
    }

    private static Stream<Tramo> tramosDeUsuario(List<Registro> ordenados) {
        return IntStream.range(0, ordenados.size() - 1)
                .mapToObj(i -> new Consecutivos(ordenados.get(i), ordenados.get(i + 1)))
                .filter(c -> c.primero().accion() == Accion.ENTRADA
                        && c.segundo().accion() == Accion.SALIDA
                        && c.primero().ruta().equals(c.segundo().ruta()))
                .map(c -> new Tramo(
                        new ParEstaciones(c.primero().estacion(), c.segundo().estacion()),
                        Duration.between(c.primero().timestamp(), c.segundo().timestamp()).toSeconds()));
    }

    // ------------------------------------------------------------------
    // f) Detección de sobrecarga en rutas
    // ------------------------------------------------------------------

    private record RutaHora(String ruta, int hora) { }

    /**
     * Calcula, por ruta, los pasajeros de su hora más cargada y los compara con su capacidad
     * simulada. Si la ocupación supera el umbral, la ruta queda marcada como crítica.
     */
    public static List<EstadoRuta> detectarSobrecarga(Stream<Registro> registros,
                                                      Map<String, Integer> capacidadPorHora,
                                                      double umbral) {
        Predicate<Double> esCritica = superaUmbral(umbral);
        return contarPor(registros, ES_ENTRADA, r -> new RutaHora(r.ruta(), r.hora()))
                .entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().ruta(), Map.Entry::getValue, Math::max))
                .entrySet().stream()
                .map(e -> {
                    int capacidad = Optional.ofNullable(capacidadPorHora.get(e.getKey()))
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Sin capacidad definida para la ruta " + e.getKey()));
                    double ocupacion = e.getValue() / (double) capacidad;
                    return new EstadoRuta(e.getKey(), e.getValue(), capacidad, ocupacion,
                            esCritica.test(ocupacion));
                })
                .sorted(Comparator.comparingDouble(EstadoRuta::ocupacion).reversed()
                        .thenComparing(EstadoRuta::ruta))
                .toList();
    }

    // ------------------------------------------------------------------
    // Utilidades privadas
    // ------------------------------------------------------------------

    private static <K extends Comparable<K>> List<Conteo<K>> aConteos(Map<K, Long> conteos,
                                                                       Comparator<Conteo<K>> orden) {
        return conteos.entrySet().stream()
                .map(e -> new Conteo<>(e.getKey(), e.getValue()))
                .sorted(orden)
                .toList();
    }

    private static <K extends Comparable<K>> Comparator<Conteo<K>> porTotalDescendente() {
        return Comparator.<Conteo<K>>comparingLong(Conteo::total).reversed()
                .thenComparing(Conteo::clave);
    }
}
