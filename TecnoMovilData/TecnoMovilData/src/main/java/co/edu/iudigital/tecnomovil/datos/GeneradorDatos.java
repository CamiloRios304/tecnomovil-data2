package co.edu.iudigital.tecnomovil.datos;

import co.edu.iudigital.tecnomovil.modelo.Accion;
import co.edu.iudigital.tecnomovil.modelo.Registro;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Generador de datos simulados para TecnoMovil Data.
 *
 * Es determinista: los mismos parámetros (usuarios, semilla, fecha) producen siempre
 * exactamente la misma lista. Cada viaje se calcula con su propio generador aleatorio
 * derivado de (semilla, usuario, viaje), de modo que no hay estado compartido.
 * El resultado es una lista inmutable (Stream.toList()).
 */
public final class GeneradorDatos {

    public static final int NUM_RUTAS = 10;
    public static final int ESTACIONES_POR_RUTA = 8;
    public static final int TOTAL_ESTACIONES = 30;

    /** Popularidad relativa de cada ruta (R01 es la más usada). */
    private static final int[] PESOS_RUTAS = {20, 16, 14, 12, 10, 8, 7, 6, 4, 3};

    /** Franjas: 0 = mañana, 1 = mediodía, 2 = tarde. */
    private static final int[][] HORAS = {{5, 6, 7, 8, 9}, {11, 12, 13, 14}, {16, 17, 18, 19, 20}};
    private static final int[][] PESOS_HORAS = {{2, 6, 10, 7, 3}, {3, 5, 5, 3}, {3, 7, 10, 6, 2}};

    private GeneradorDatos() { }

    /** Nombres de las rutas: R01 ... R10. */
    public static List<String> rutas() {
        return IntStream.rangeClosed(1, NUM_RUTAS)
                .mapToObj(i -> String.format("R%02d", i))
                .toList();
    }

    /** Capacidad simulada (pasajeros que la ruta puede mover por hora). */
    public static Map<String, Integer> capacidadesSimuladas() {
        return Map.of(
                "R01", 8000, "R02", 6400, "R03", 6000, "R04", 5200, "R05", 4600,
                "R06", 3800, "R07", 3400, "R08", 3000, "R09", 2000, "R10", 1200);
    }

    /** Genera dos registros (entrada y salida) por cada viaje de cada usuario. */
    public static List<Registro> generar(int usuarios, long semilla, LocalDate fecha) {
        return IntStream.range(0, usuarios)
                .boxed()
                .flatMap(u -> registrosDeUsuario(u, semilla, fecha))
                .toList();
    }

    private static Stream<Registro> registrosDeUsuario(int usuario, long semilla, LocalDate fecha) {
        int sorteo = new SplittableRandom(mezclar(semilla, usuario, -1)).nextInt(10);
        int viajes = sorteo < 2 ? 1 : sorteo < 8 ? 2 : 3;
        return IntStream.range(0, viajes)
                .boxed()
                .flatMap(k -> viaje(usuario, k, viajes, semilla, fecha));
    }

    private static Stream<Registro> viaje(int usuario, int k, int viajes, long semilla, LocalDate fecha) {
        SplittableRandom rnd = new SplittableRandom(mezclar(semilla, usuario, k));
        String id = String.format("U%06d", usuario + 1);
        int franja = franja(viajes, k);

        int indiceRuta = elegirPonderado(rnd, PESOS_RUTAS);
        String ruta = String.format("R%02d", indiceRuta + 1);

        int origen = rnd.nextInt(ESTACIONES_POR_RUTA - 1);
        int destino = origen + 1 + rnd.nextInt(ESTACIONES_POR_RUTA - 1 - origen);

        int hora = HORAS[franja][elegirPonderado(rnd, PESOS_HORAS[franja])];
        LocalDateTime inicio = fecha.atTime(hora, rnd.nextInt(60), rnd.nextInt(60));
        long duracionSegundos = (destino - origen) * 180L + 60 + rnd.nextInt(240);

        return Stream.of(
                new Registro(id, ruta, estacion(indiceRuta, origen), Accion.ENTRADA, inicio),
                new Registro(id, ruta, estacion(indiceRuta, destino), Accion.SALIDA,
                        inicio.plusSeconds(duracionSegundos)));
    }

    /** Estación ubicada en la posición dada de una ruta (las rutas comparten estaciones). */
    private static String estacion(int indiceRuta, int posicion) {
        return String.format("E%02d", (indiceRuta * 3 + posicion) % TOTAL_ESTACIONES + 1);
    }

    private static int franja(int viajes, int k) {
        return switch (viajes) {
            case 1 -> 0;
            case 2 -> k == 0 ? 0 : 2;
            default -> k;
        };
    }

    /** Elige un índice con probabilidad proporcional a su peso (declarativo, sin bucles). */
    private static int elegirPonderado(SplittableRandom rnd, int[] pesos) {
        int sorteo = rnd.nextInt(Arrays.stream(pesos).sum());
        return IntStream.range(0, pesos.length)
                .filter(i -> Arrays.stream(pesos, 0, i + 1).sum() > sorteo)
                .findFirst()
                .orElseThrow();
    }

    private static long mezclar(long semilla, int usuario, int viaje) {
        return semilla ^ (0x9E3779B97F4A7C15L * (usuario + 1L)) ^ (0xC2B2AE3D27D4EB4FL * (viaje + 2L));
    }
}
