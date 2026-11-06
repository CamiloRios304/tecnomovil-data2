package co.edu.iudigital.tecnomovil.app;

import co.edu.iudigital.tecnomovil.analisis.AnalizadorTransporte;
import co.edu.iudigital.tecnomovil.analisis.Conteo;
import co.edu.iudigital.tecnomovil.analisis.EstadoRuta;
import co.edu.iudigital.tecnomovil.analisis.TiempoTramo;
import co.edu.iudigital.tecnomovil.datos.GeneradorDatos;
import co.edu.iudigital.tecnomovil.modelo.Registro;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Programa principal: genera datos simulados, ejecuta las seis operaciones funcionales
 * de forma secuencial y paralela, y demuestra la inmutabilidad. Toda la salida por
 * consola ocurre aquí; el módulo de análisis no produce efectos secundarios.
 *
 * Uso: TecnoMovilDataApp [numeroDeUsuarios]   (por defecto 100000)
 */
public final class TecnoMovilDataApp {

    private static final Locale LOCALE = Locale.forLanguageTag("es-CO");
    private static final long SEMILLA = 2026L;
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 2);
    private static final double UMBRAL_OCUPACION = 0.85;

    /** Resultado completo del análisis; al ser record se compara por valor con equals. */
    private record Resultados(List<Conteo<String>> afluencia,
                              List<Conteo<Integer>> flujoPorHora,
                              List<Conteo<Integer>> horasPico,
                              List<Conteo<String>> rutas,
                              Map<String, List<String>> patrones,
                              List<TiempoTramo> tiempos,
                              double tiempoGlobal,
                              List<EstadoRuta> sobrecarga) { }

    private TecnoMovilDataApp() { }

    /** Función pura: mismas entradas, mismos resultados; no modifica la fuente. */
    private static Resultados analizar(Supplier<Stream<Registro>> fuente) {
        List<Conteo<Integer>> flujo = AnalizadorTransporte.flujoPorHora(fuente.get());
        return new Resultados(
                AnalizadorTransporte.afluenciaPorEstacion(fuente.get()),
                flujo,
                AnalizadorTransporte.horasPico(flujo, 3),
                AnalizadorTransporte.rutasMasUtilizadas(fuente.get(), 5),
                AnalizadorTransporte.patronesDeViaje(fuente.get()),
                AnalizadorTransporte.tiempoPromedioEntreEstaciones(fuente.get()),
                AnalizadorTransporte.tiempoPromedioGlobal(fuente.get()),
                AnalizadorTransporte.detectarSobrecarga(fuente.get(),
                        GeneradorDatos.capacidadesSimuladas(), UMBRAL_OCUPACION));
    }

    public static void main(String[] args) {
        int usuarios = args.length > 0 ? Integer.parseInt(args[0]) : 100_000;

        titulo("TecnoMovil Data - Procesamiento funcional de datos de transporte");
        long inicioGeneracion = System.nanoTime();
        List<Registro> registros = GeneradorDatos.generar(usuarios, SEMILLA, FECHA);
        long msGeneracion = (System.nanoTime() - inicioGeneracion) / 1_000_000;
        int huellaInicial = registros.hashCode();

        imprimir("Fecha simulada        : %s", FECHA);
        imprimir("Usuarios simulados    : %,d", usuarios);
        imprimir("Registros generados   : %,d (generados en %d ms)", registros.size(), msGeneracion);
        imprimir("Núcleos disponibles   : %d", Runtime.getRuntime().availableProcessors());

        Resultados r = analizar(registros::stream);

        titulo("a) Afluencia por estación (ingresos del día) - Top 10");
        r.afluencia().stream().limit(10).forEach(c -> imprimir("  %-6s %,10d", c.clave(), c.total()));

        titulo("b) Identificación de horas pico (ingresos por hora)");
        long maximo = r.flujoPorHora().stream().mapToLong(Conteo::total).max().orElse(1);
        r.flujoPorHora().forEach(c -> imprimir("  %02d:00 %,8d %s", c.clave(), c.total(),
                "#".repeat((int) (30 * c.total() / maximo))));
        imprimir("  Horas pico (top 3): %s", r.horasPico().stream()
                .map(c -> String.format(LOCALE, "%02d:00 (%,d)", c.clave(), c.total()))
                .toList());

        titulo("c) Rutas más utilizadas - Top 5");
        r.rutas().forEach(c -> imprimir("  %-5s %,10d ingresos", c.clave(), c.total()));

        titulo("d) Patrones de viaje por usuario (primeros 5 usuarios)");
        r.patrones().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(5)
                .forEach(e -> imprimir("  %s -> %s", e.getKey(), String.join(" > ", e.getValue())));

        titulo("e) Tiempo promedio entre estaciones - 8 tramos con más muestras");
        r.tiempos().stream().limit(8).forEach(t -> imprimir("  %s -> %s  %6.2f min  (%,d viajes)",
                t.origen(), t.destino(), t.minutosPromedio(), t.muestras()));
        imprimir("  Tiempo promedio global: %.2f min", r.tiempoGlobal());

        titulo("f) Detección de sobrecarga (umbral de ocupación = %.0f%%)", UMBRAL_OCUPACION * 100);
        imprimir("  %-5s %12s %10s %10s  %s", "Ruta", "Pico/hora", "Capacidad", "Ocupación", "Estado");
        r.sobrecarga().forEach(e -> imprimir("  %-5s %,12d %,10d %9.1f%%  %s",
                e.ruta(), e.pasajerosHoraPico(), e.capacidadHora(), e.ocupacion() * 100,
                e.critica() ? "CRÍTICA" : "normal"));

        titulo("Secuencial vs. parallelStream()");
        medir(registros);
        Resultados paralelo = analizar(registros::parallelStream);
        imprimir("  Resultado paralelo == secuencial : %s", r.equals(paralelo));
        imprimir("  (sin condiciones de carrera: no hay estado mutable compartido)");

        titulo("Demostración de inmutabilidad");
        imprimir("  Huella de la lista original antes   : %d", huellaInicial);
        imprimir("  Huella de la lista original después : %d", registros.hashCode());
        imprimir("  Lista original sin cambios          : %s", huellaInicial == registros.hashCode());
        intentarModificar("registros.add(...)", () -> registros.add(registros.get(0)));
        intentarModificar("afluencia.clear()", () -> r.afluencia().clear());
        intentarModificar("patrones.put(...)", () -> r.patrones().put("U000000", List.of()));
    }

    private static void medir(List<Registro> registros) {
        analizar(registros::stream);          // calentamiento de la JVM
        analizar(registros::parallelStream);
        long secuencial = tiempoMs(() -> analizar(registros::stream));
        long paralelo = tiempoMs(() -> analizar(registros::parallelStream));
        imprimir("  Tiempo secuencial : %,d ms", secuencial);
        imprimir("  Tiempo paralelo   : %,d ms", paralelo);
    }

    private static long tiempoMs(Runnable tarea) {
        long mejor = IntStream.range(0, 3)
                .mapToLong(i -> {
                    long inicio = System.nanoTime();
                    tarea.run();
                    return System.nanoTime() - inicio;
                })
                .min().orElse(0);
        return mejor / 1_000_000;
    }

    private static void intentarModificar(String operacion, Runnable intento) {
        try {
            intento.run();
            imprimir("  %-22s -> ¡se permitió modificar! (error)", operacion);
        } catch (UnsupportedOperationException e) {
            imprimir("  %-22s -> UnsupportedOperationException (colección inmutable)", operacion);
        }
    }

    private static void titulo(String formato, Object... args) {
        System.out.println();
        System.out.println(String.format(LOCALE, formato, args));
        System.out.println("-".repeat(70));
    }

    private static void imprimir(String formato, Object... args) {
        System.out.println(String.format(LOCALE, formato, args));
    }
}
