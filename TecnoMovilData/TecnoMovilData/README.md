# TecnoMovil Data – Procesamiento funcional de datos de transporte urbano

Solución en **Java 17+ (sin frameworks externos)** del caso de estudio de la Unidad 2
(Tecnología en Desarrollo de Software, IU Digital de Antioquia).

El módulo procesa registros simulados de validadores y sensores usando **funciones puras,
colecciones inmutables, lambdas, funciones de orden superior y Streams**.

## Estructura

```
src/main/java/co/edu/iudigital/tecnomovil/
├── modelo/     Registro (record inmutable) y Accion (enum)
├── datos/      GeneradorDatos: datos simulados deterministas
├── analisis/   AnalizadorTransporte: las 6 operaciones funcionales + records de resultado
└── app/        TecnoMovilDataApp: programa principal (única clase con salida por consola)
src/test/java/co/edu/iudigital/tecnomovil/
└── PruebasFuncionales.java   pruebas sin frameworks
```

## Operaciones implementadas (sección 3.2 del caso)

| Tarea | Método |
|-------|--------|
| a) Afluencia por estación | `afluenciaPorEstacion` |
| b) Horas pico | `flujoPorHora`, `horasPico` |
| c) Rutas más utilizadas | `rutasMasUtilizadas` |
| d) Patrones de viaje por usuario | `patronesDeViaje` |
| e) Tiempo promedio entre estaciones | `tiempoPromedioEntreEstaciones`, `tiempoPromedioGlobal` |
| f) Sobrecarga en rutas | `detectarSobrecarga` |

## Compilar y ejecutar

Linux / macOS:
```bash
./compilar.sh
./probar.sh              # pruebas funcionales
./ejecutar.sh            # 100.000 usuarios (~400.000 registros)
./ejecutar.sh 500000     # volumen personalizado
```
Windows: `compilar.bat`, `probar.bat`, `ejecutar.bat`.

## Decisiones de diseño

- Todas las funciones de análisis reciben un `Stream<Registro>`; el llamador elige
  `lista.stream()` o `lista.parallelStream()`. El resultado es idéntico (verificado en la ejecución).
- Los resultados se devuelven en `List.copyOf`, `Map.copyOf`, `Stream.toList()` (inmutables).
- Los ingresos (`entrada`) son la base de afluencia, horas pico, rutas y sobrecarga.
- La ocupación de una ruta = pasajeros de su hora más cargada / capacidad horaria simulada;
  es "crítica" si supera el 85 %.
