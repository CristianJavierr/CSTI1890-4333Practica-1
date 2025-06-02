import java.io.*;
import java.nio.file.*;
import java.util.Random;
import java.util.concurrent.*;
import java.util.List;

public class Main {

    //Genera un archivo de texto con un número dado de enteros aleatorios

    private static void generarArchivoDatos(String filename, int size) throws IOException {
        Random rand = new Random();
        // Abre un writer para el archivo destino
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filename))) {
            for (int i = 0; i < size; i++) {
                // Genera y escribe un entero aleatorio
                int numero = rand.nextInt(10_000) + 1;
                // Cada línea la genere para que conteniera  un número entre 1 y 10,000
                writer.write(Integer.toString(numero));
                writer.newLine();
            }
        }
    }

    //Lee todos los enteros de un archivo de texto, uno por líne
    private static int[] leerDatos(String filename) throws IOException {
        List<String> lineas = Files.readAllLines(Paths.get(filename));
        int[] datos = new int[lineas.size()];
        for (int i = 0; i < lineas.size(); i++) {
            // Convierte cada línea a entero
            datos[i] = Integer.parseInt(lineas.get(i));
        }
        return datos;
    }

    //Realiza la suma de forma secuencial para to o el array
    private static long sumaSecuencial(int[] datos) {
        long suma = 0;
        for (int v : datos) {
            suma += v;  // Acumula cada valor en la variable suma
        }
        return suma;
    }

    //Realiza la suma de forma paralela dividiendo el trabajo entre varios hilos.
    private static long sumaParalela(int[] datos, int numHilos) throws InterruptedException, ExecutionException {
        int n = datos.length;
        // Crea un pool de hilos con el número solicitado
        ExecutorService exec = Executors.newFixedThreadPool(numHilos);

        // Tamaño del bloque de datos para cada hilo
        int bloque = (n + numHilos - 1) / numHilos;
        List<Future<Long>> resultados = new CopyOnWriteArrayList<>();

        for (int i = 0; i < numHilos; i++) {
            final int inicio = i * bloque;
            final int fin   = Math.min(inicio + bloque, n);
            if (inicio >= fin) break;  // No hay datos para este hilo

            // Envía la tarea de suma parcial al pool
            resultados.add(exec.submit(() -> {
                long parcial = 0;
                for (int j = inicio; j < fin; j++) {
                    parcial += datos[j];  // Suma dentro de cada subrango
                }
                return parcial;
            }));
        }

        long sumaTotal = 0;
        // Espera a que cada hilo termine y acumula sus resultados
        for (Future<Long> f : resultados) {
            sumaTotal += f.get();
        }
        // Apaga el pool de hilos
        exec.shutdown();
        return sumaTotal;
    }

    public static void main(String[] args) throws Exception {
        final String FILENAME = "datos.txt";
        final int SIZE = 1_000_000;  // Número de datos a generar

        // 1) Generar el archivo de datos si no existe
        if (!Files.exists(Paths.get(FILENAME))) {
            System.out.println("Generando archivo de datos");
            generarArchivoDatos(FILENAME, SIZE);
        }

        // 2) Leer datos desde el archivo a un array en memoria
        System.out.println("Cargando datos en memoria");
        int[] datos = leerDatos(FILENAME);

        // Ejecutar y medir la suma secuencial
        System.out.println("suma secuencial");
        long t0 = System.nanoTime();
        long sumaSec = sumaSecuencial(datos);
        double tiempoSec = (System.nanoTime() - t0) / 1e9;
        System.out.printf("Resultado: %d, Tiempo secuencial: %.6f s%n", sumaSec, tiempoSec);

        // Ejecutar y medir la suma paralela con distintos conteos de hilos
        int[] hilosTest = {2, 4, 8};
        System.out.println("\nHilos\tTiempo(s)\tSpeedup\tEficiencia");
        for (int h : hilosTest) {
            t0 = System.nanoTime();
            long sumaPar = sumaParalela(datos, h);
            double tiempoPar = (System.nanoTime() - t0) / 1e9;
            double speedup = tiempoSec / tiempoPar;  // Ganancia de velocidad
            double eficiencia = speedup / h;         // Eficiencia por hilo

            // Verifica integridad del cálculo
            if (sumaPar != sumaSec) {
                System.err.println("¡ERROR: la suma paralela no coincide con la secuencial!");
                return;
            }

            // Muestra resultados formateados
            System.out.printf("%d\t%.6f\t%.2f\t%.2f%n", h, tiempoPar, speedup, eficiencia);
        }
    }
}
