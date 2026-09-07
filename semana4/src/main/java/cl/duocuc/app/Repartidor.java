package cl.duocuc.app;

import cl.duocuc.interfaces.Cancelable;
import cl.duocuc.interfaces.Despachable;
import cl.duocuc.interfaces.Rastreable;
import cl.duocuc.model.Pedido;

import java.util.ArrayList;
import java.util.List;

/**
 * Repartidor que entrega su propia lista de pedidos dentro de un hilo
 * independiente. Cada instancia se ejecuta como una tarea {@link Runnable},
 * por lo que varios repartidores pueden entregar de manera simultánea.
 */
public class Repartidor implements Runnable, Despachable, Cancelable, Rastreable {

    private final String nombre;
    private final List<Pedido> pedidos;
    private final List<String> historial = new ArrayList<>();
    private Pedido pedidoActual;
    private long tiempoTrabajadoMs;
    private volatile boolean cancelado;

    /**
     * Crea un repartidor con la lista de pedidos que debe entregar.
     *
     * @param nombre  nombre del repartidor
     * @param pedidos pedidos asignados, en el orden en que serán entregados
     */
    public Repartidor(String nombre, List<Pedido> pedidos) {
        this.nombre = nombre;
        this.pedidos = pedidos;
    }

    /**
     * Obtiene el nombre del repartidor.
     *
     * @return el nombre del repartidor
     */
    public String getNombre() { return nombre; }

    /**
     * Obtiene los pedidos asignados al repartidor.
     *
     * @return la lista de pedidos asignados, en el orden de entrega
     */
    public List<Pedido> getPedidos() { return List.copyOf(pedidos); }

    /**
     * Obtiene el tiempo que el repartidor invirtió en sus entregas.
     *
     * @return el tiempo acumulado de entrega, en milisegundos
     */
    public long getTiempoTrabajadoMs() { return tiempoTrabajadoMs; }

    /**
     * Entrega de manera secuencial todos los pedidos asignados al repartidor.
     * Este método es el cuerpo del hilo: se detiene si el repartidor fue
     * cancelado o si el hilo recibe una interrupción.
     */
    @Override
    public void run() {
        for (Pedido pedido : pedidos) {
            if (cancelado || Thread.currentThread().isInterrupted()) {
                System.out.println("[Repartidor: " + nombre + "] Entregas restantes canceladas.");
                return;
            }
            pedidoActual = pedido;
            despachar();
        }
        System.out.println("[Repartidor: " + nombre + "] Terminó todas sus entregas.");
    }

    /**
     * Entrega el pedido en curso: informa el avance por consola, simula el
     * tiempo de traslado con {@link Thread#sleep(long)} y registra la entrega
     * en el historial. No hace nada si no hay pedido en curso.
     */
    @Override
    public void despachar() {
        if (pedidoActual == null) return;

        String identificacion = pedidoActual.getClass().getSimpleName()
                + " #" + pedidoActual.getIdPedido();
        System.out.println("[Repartidor: " + nombre + "] Entregando " + identificacion + "...");

        pedidoActual.asignarRepartidor(nombre);
        long demora = (long) (Math.random() * 2000 + 1000);
        try {
            Thread.sleep(demora);
            tiempoTrabajadoMs += demora;
        } catch (InterruptedException e) {
            System.out.println("[Repartidor: " + nombre + "] Entrega de "
                    + identificacion + " interrumpida.");
            Thread.currentThread().interrupt();
            return;
        }

        historial.add(identificacion + " - " + pedidoActual.getDireccionEntrega()
                + " (" + pedidoActual.calcularTiempoEntrega() + " min estimados)");
        System.out.println("[Repartidor: " + nombre + "] Pedido #"
                + pedidoActual.getIdPedido() + " entregado.");
    }

    /**
     * Cancela las entregas que el repartidor aún no ha iniciado. La entrega en
     * curso se completa antes de detener el hilo.
     */
    @Override
    public void cancelar() {
        cancelado = true;
        System.out.println("[Repartidor: " + nombre + "] Solicitud de cancelación recibida.");
    }

    /**
     * Imprime en consola las entregas que el repartidor completó.
     */
    @Override
    public void verHistorial() {
        System.out.println("Historial de " + nombre + ":");
        if (historial.isEmpty()) {
            System.out.println("- Sin entregas registradas.");
            return;
        }
        for (String entrada : historial) {
            System.out.println("- " + entrada);
        }
    }
}
