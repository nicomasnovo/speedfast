package cl.duocuc.app;

import cl.duocuc.model.Pedido;
import cl.duocuc.model.PedidoComida;
import cl.duocuc.model.PedidoEncomienda;
import cl.duocuc.model.PedidoExpress;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Punto de entrada de la aplicación SpeedFast.
 * Simula varios repartidores entregando sus pedidos al mismo tiempo: cada
 * {@link Repartidor} es una tarea {@link Runnable} que se ejecuta en su propio
 * hilo mediante un {@link ExecutorService}.
 */
public class Main {

    /** Línea usada para separar los distintos casos de la simulación. */
    private static final String SEPARADOR = "----------------------------------------";

    /** Cantidad de hilos del pool: uno por repartidor. */
    private static final int TOTAL_REPARTIDORES = 3;

    /** Tiempo máximo de espera para que el pool termine sus entregas. */
    private static final int ESPERA_MAXIMA_SEGUNDOS = 60;

    /**
     * Ejecuta la demostración completa de la aplicación.
     *
     * @param args argumentos de línea de comandos; no se utilizan
     * @throws InterruptedException si el hilo principal es interrumpido mientras
     *                              espera que los repartidores terminen
     */
    public static void main(String[] args) throws InterruptedException {

        Repartidor camila = new Repartidor("Camila", List.of(
                new PedidoComida(101, "Av. Italia 456", 4, "La Pizzería", 15),
                new PedidoComida(104, "Los Leones 2100", 6, "Sushi Norte", 20)));

        Repartidor luis = new Repartidor("Luis", List.of(
                new PedidoExpress(102, "Av. Apoquindo 1500", 7, "TechStore"),
                new PedidoExpress(105, "Providencia 890", 3, "FarmaExpress")));

        Repartidor pedro = new Repartidor("Pedro", List.of(
                new PedidoEncomienda(103, "Av. Santa Rosa 567", 7, 3.5, 8.0),
                new PedidoEncomienda(106, "San Pablo 3200", 12, 9.0, 25.0)));

        List<Repartidor> repartidores = List.of(camila, luis, pedro);

        System.out.println("[Pedidos asignados]\n");
        for (Repartidor repartidor : repartidores) {
            System.out.println("Repartidor: " + repartidor.getNombre());
            for (Pedido pedido : repartidor.getPedidos()) {
                pedido.mostrarResumen();
                System.out.println();
            }
        }
        System.out.println(SEPARADOR + "\n");

        System.out.println("[Entregas en paralelo con ExecutorService]\n");
        long inicio = System.currentTimeMillis();

        ExecutorService executor = Executors.newFixedThreadPool(TOTAL_REPARTIDORES);
        for (Repartidor repartidor : repartidores) {
            executor.submit(repartidor);
        }
        executor.shutdown();
        if (!executor.awaitTermination(ESPERA_MAXIMA_SEGUNDOS, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }

        long duracionParalela = System.currentTimeMillis() - inicio;
        System.out.println("\nTodos los repartidores han finalizado sus entregas.");
        System.out.println("\n" + SEPARADOR + "\n");

        System.out.println("[Historial de entregas]\n");
        for (Repartidor repartidor : repartidores) {
            repartidor.verHistorial();
            System.out.println();
        }
        System.out.println(SEPARADOR + "\n");

        System.out.println("[Impacto de la concurrencia en el rendimiento]\n");
        long duracionSecuencial = 0;
        for (Repartidor repartidor : repartidores) {
            long trabajado = repartidor.getTiempoTrabajadoMs();
            duracionSecuencial += trabajado;
            System.out.println("- " + repartidor.getNombre() + " entregó en "
                    + formatearSegundos(trabajado) + " s.");
        }
        System.out.println("Tiempo si las entregas fueran secuenciales: "
                + formatearSegundos(duracionSecuencial) + " s.");
        System.out.println("Tiempo real con " + TOTAL_REPARTIDORES + " hilos en paralelo: "
                + formatearSegundos(duracionParalela) + " s.");
        System.out.println("Mejora obtenida: "
                + String.format("%.2f", (double) duracionSecuencial / duracionParalela) + "x.");
        System.out.println("\n" + SEPARADOR + "\n");

        System.out.println("[Cancelación de un repartidor en ejecución]\n");
        Repartidor sofia = new Repartidor("Sofía", List.of(
                new PedidoComida(107, "Manuel Montt 45", 5, "Café Central", 10),
                new PedidoExpress(108, "Irarrázaval 1200", 8, "TechStore"),
                new PedidoEncomienda(109, "Gran Avenida 4500", 15, 2.0, 5.0)));

        Thread hiloSofia = new Thread(sofia, "hilo-" + sofia.getNombre());
        hiloSofia.start();
        Thread.sleep(1500);
        sofia.cancelar();
        hiloSofia.join();

        System.out.println();
        sofia.verHistorial();
        System.out.println("\n" + SEPARADOR);
    }

    /**
     * Convierte una duración en milisegundos a segundos con un decimal.
     *
     * @param milisegundos duración a formatear, en milisegundos
     * @return la duración expresada en segundos
     */
    private static String formatearSegundos(long milisegundos) {
        return String.format("%.1f", milisegundos / 1000.0);
    }
}
