package cl.duocuc.app;

import cl.duocuc.model.Pedido;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Punto de entrada de la aplicación SpeedFast.
 * Simula el despacho de pedidos desde una {@link ZonaDeCarga} compartida: los
 * pedidos llegan en estado PENDIENTE y tres {@link Repartidor} los retiran de
 * forma sincronizada, cada uno en su propio hilo dentro de un
 * {@link ExecutorService}.
 */
public class Main {

    /** Línea usada para separar las etapas de la simulación. */
    private static final String SEPARADOR = "----------------------------------------";

    /** Nombres de los repartidores; hay un hilo por cada uno. */
    private static final String[] NOMBRES_REPARTIDORES = {"Camila", "Luis", "Pedro"};

    /** Tiempo máximo de espera para que el pool termine las entregas. */
    private static final int ESPERA_MAXIMA_SEGUNDOS = 60;

    /**
     * Ejecuta la simulación completa del despacho concurrente.
     *
     * @param args argumentos de línea de comandos; no se utilizan
     * @throws InterruptedException si el hilo principal es interrumpido mientras
     *                              espera que los repartidores terminen
     */
    public static void main(String[] args) throws InterruptedException {

        ZonaDeCarga zonaDeCarga = new ZonaDeCarga();

        System.out.println("[Llegada de pedidos a la zona de carga]\n");
        zonaDeCarga.agregarPedido(new Pedido(101, "Av. Italia 456"));
        zonaDeCarga.agregarPedido(new Pedido(102, "Av. Apoquindo 1500"));
        zonaDeCarga.agregarPedido(new Pedido(103, "Av. Santa Rosa 567"));
        zonaDeCarga.agregarPedido(new Pedido(104, "Los Leones 2100"));
        zonaDeCarga.agregarPedido(new Pedido(105, "Providencia 890"));
        zonaDeCarga.agregarPedido(new Pedido(106, "San Pablo 3200"));
        System.out.println("\n" + SEPARADOR + "\n");

        System.out.println("[Pedidos en espera]\n");
        for (Pedido pedido : zonaDeCarga.getPedidos()) {
            System.out.println(pedido);
        }
        System.out.println("\n" + SEPARADOR + "\n");

        System.out.println("[Reparto concurrente con ExecutorService]\n");
        List<Repartidor> repartidores = new ArrayList<>();
        for (String nombre : NOMBRES_REPARTIDORES) {
            repartidores.add(new Repartidor(nombre, zonaDeCarga));
        }

        ExecutorService executor = Executors.newFixedThreadPool(repartidores.size());
        for (Repartidor repartidor : repartidores) {
            executor.submit(repartidor);
        }
        executor.shutdown();
        if (!executor.awaitTermination(ESPERA_MAXIMA_SEGUNDOS, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }
        System.out.println("\n" + SEPARADOR + "\n");

        System.out.println("[Resumen del despacho]\n");
        for (Repartidor repartidor : repartidores) {
            System.out.println("- " + repartidor.getNombre() + " entregó "
                    + repartidor.getEntregas() + " pedido(s).");
        }
        System.out.println();
        for (Pedido pedido : zonaDeCarga.getPedidos()) {
            System.out.println(pedido);
        }
        System.out.println("\nEntregas registradas: " + zonaDeCarga.getEntregados()
                + " de " + zonaDeCarga.getTotalPedidos() + " pedidos.");

        System.out.println();
        if (zonaDeCarga.todosEntregados()) {
            System.out.println("Todos los pedidos han sido entregados correctamente.");
        } else {
            System.out.println("Quedaron pedidos sin entregar en la zona de carga.");
        }
    }
}
