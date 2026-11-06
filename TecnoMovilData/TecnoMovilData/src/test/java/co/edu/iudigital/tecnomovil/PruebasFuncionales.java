package co.edu.iudigital.tecnomovil;

import co.edu.iudigital.tecnomovil.analisis.AnalizadorTransporte;
import co.edu.iudigital.tecnomovil.analisis.Conteo;
import co.edu.iudigital.tecnomovil.analisis.EstadoRuta;
import co.edu.iudigital.tecnomovil.analisis.TiempoTramo;
import co.edu.iudigital.tecnomovil.datos.GeneradorDatos;
import co.edu.iudigital.tecnomovil.modelo.Accion;
import co.edu.iudigital.tecnomovil.modelo.Registro;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Pruebas sin frameworks externos: cada verificación imprime OK o FALLO. */
public final class PruebasFuncionales {

    private static int fallos = 0;

    private static final LocalDate DIA = LocalDate.of(2026, 11, 2);

    private static Registro reg(String u, String ruta, String est, Accion a, int h, int m) {
        return new Registro(u, ruta, est, a, LocalDateTime.of(DIA, java.time.LocalTime.of(h, m)));
    }

    private static final List<Registro> DATOS = List.of(
            reg("U1", "R01", "E01", Accion.ENTRADA, 7, 0),
            reg("U1", "R01", "E03", Accion.SALIDA, 7, 10),
            reg("U1", "R01", "E03", Accion.ENTRADA, 18, 0),
            reg("U1", "R01", "E01", Accion.SALIDA, 18, 30),
            reg("U2", "R02", "E01", Accion.ENTRADA, 7, 30),
            reg("U2", "R02", "E02", Accion.SALIDA, 7, 45),
            reg("U3", "R01", "E01", Accion.ENTRADA, 8, 0),
            reg("U3", "R01", "E04", Accion.SALIDA, 8, 20));

    public static void main(String[] args) {
        System.out.println("Pruebas funcionales - TecnoMovil Data");
        System.out.println("-".repeat(60));

        List<Conteo<String>> afluencia = AnalizadorTransporte.afluenciaPorEstacion(DATOS.stream());
        verificar("a) afluencia: E01 lidera con 3 ingresos",
                afluencia.get(0).equals(new Conteo<>("E01", 3L)));
        verificar("a) afluencia: E03 tiene 1 ingreso",
                afluencia.contains(new Conteo<>("E03", 1L)));

        List<Conteo<Integer>> flujo = AnalizadorTransporte.flujoPorHora(DATOS.stream());
        verificar("b) horas: 7h=2, 8h=1, 18h=1",
                flujo.equals(List.of(new Conteo<>(7, 2L), new Conteo<>(8, 1L), new Conteo<>(18, 1L))));
        verificar("b) hora pico es las 7:00",
                AnalizadorTransporte.horasPico(flujo, 1).get(0).clave() == 7);

        verificar("c) ruta más usada: R01 con 3 ingresos",
                AnalizadorTransporte.rutasMasUtilizadas(DATOS.stream(), 1)
                        .equals(List.of(new Conteo<>("R01", 3L))));

        Map<String, List<String>> patrones = AnalizadorTransporte.patronesDeViaje(DATOS.stream());
        verificar("d) patrón de U1 en orden cronológico",
                patrones.get("U1").equals(List.of("E01", "E03", "E03", "E01")));
        verificar("d) patrón de U2", patrones.get("U2").equals(List.of("E01", "E02")));

        List<TiempoTramo> tiempos = AnalizadorTransporte.tiempoPromedioEntreEstaciones(DATOS.stream());
        verificar("e) tramo E01->E03 = 10 min",
                tiempos.stream().anyMatch(t -> t.origen().equals("E01") && t.destino().equals("E03")
                        && t.minutosPromedio() == 10.0));
        verificar("e) promedio global = 18,75 min",
                AnalizadorTransporte.tiempoPromedioGlobal(DATOS.stream()) == 18.75);

        List<EstadoRuta> estados = AnalizadorTransporte.detectarSobrecarga(
                DATOS.stream(), Map.of("R01", 1, "R02", 10), 0.8);
        verificar("f) R01 (capacidad 1) es crítica",
                estados.stream().anyMatch(e -> e.ruta().equals("R01") && e.critica()));
        verificar("f) R02 (capacidad 10) es normal",
                estados.stream().anyMatch(e -> e.ruta().equals("R02") && !e.critica()));

        verificar("pureza: dos ejecuciones dan el mismo resultado",
                AnalizadorTransporte.afluenciaPorEstacion(DATOS.stream())
                        .equals(AnalizadorTransporte.afluenciaPorEstacion(DATOS.stream())));

        List<Registro> grande = GeneradorDatos.generar(20_000, 7L, DIA);
        verificar("paralelo == secuencial (afluencia)",
                AnalizadorTransporte.afluenciaPorEstacion(grande.stream())
                        .equals(AnalizadorTransporte.afluenciaPorEstacion(grande.parallelStream())));
        verificar("paralelo == secuencial (patrones)",
                AnalizadorTransporte.patronesDeViaje(grande.stream())
                        .equals(AnalizadorTransporte.patronesDeViaje(grande.parallelStream())));
        verificar("paralelo == secuencial (tiempos)",
                AnalizadorTransporte.tiempoPromedioEntreEstaciones(grande.stream())
                        .equals(AnalizadorTransporte.tiempoPromedioEntreEstaciones(grande.parallelStream())));
        verificar("generador determinista (misma semilla, misma lista)",
                grande.equals(GeneradorDatos.generar(20_000, 7L, DIA)));

        int huella = grande.hashCode();
        AnalizadorTransporte.patronesDeViaje(grande.parallelStream());
        verificar("inmutabilidad: la lista original no cambia", huella == grande.hashCode());
        boolean rechazo;
        try {
            grande.add(grande.get(0));
            rechazo = false;
        } catch (UnsupportedOperationException e) {
            rechazo = true;
        }
        verificar("inmutabilidad: add() lanza UnsupportedOperationException", rechazo);

        System.out.println("-".repeat(60));
        System.out.println(fallos == 0 ? "TODAS LAS PRUEBAS PASARON" : fallos + " PRUEBA(S) FALLARON");
        if (fallos > 0) {
            System.exit(1);
        }
    }

    private static void verificar(String nombre, boolean condicion) {
        System.out.println((condicion ? "[OK]    " : "[FALLO] ") + nombre);
        if (!condicion) {
            fallos++;
        }
    }
}
