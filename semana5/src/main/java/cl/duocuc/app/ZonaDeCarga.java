package cl.duocuc.app;

import cl.duocuc.model.EstadoPedido;
import cl.duocuc.model.Pedido;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Zona de carga de SpeedFast: el recurso compartido desde el que los
 * repartidores retiran los pedidos.
 * <p>
 * Los pedidos disponibles se guardan en un {@link BlockingQueue}, que garantiza
 * que cada elemento sea entregado a un solo hilo: dos repartidores nunca pueden
 * retirar el mismo pedido, con lo que se evitan las condiciones de carrera y las
 * entregas duplicadas. El registro de pedidos y el contador de entregas se
 * protegen con {@code synchronized}, porque son leídos y escritos desde
 * distintos hilos.
 */
public class ZonaDeCarga {

    /** Tiempo que un repartidor espera por un pedido antes de dar por vacía la zona. */
    private static final long ESPERA_MAXIMA_MS = 500;

    private final BlockingQueue<Pedido> cola = new LinkedBlockingQueue<>();
    private final List<Pedido> registro = new ArrayList<>();
    private int entregados;

    /**
     * Deja un pedido disponible en la zona de carga e informa su llegada.
     *
     * @param pedido pedido que ingresa a la zona de carga
     */
    public void agregarPedido(Pedido pedido) {
        synchronized (this) {
            registro.add(pedido);
        }
        cola.add(pedido);
        System.out.println("[ZonaDeCarga] Pedido #" + pedido.getId() + " disponible");
    }

    /**
     * Retira un pedido de la zona de carga. La operación es atómica: el pedido
     * que devuelve este método queda fuera de la cola, por lo que ningún otro
     * repartidor puede tomarlo.
     *
     * @return el siguiente pedido disponible, o {@code null} si la zona de carga
     *         sigue vacía tras esperar {@value #ESPERA_MAXIMA_MS} milisegundos
     * @throws InterruptedException si el hilo es interrumpido mientras espera un pedido
     */
    public Pedido retirarPedido() throws InterruptedException {
        return cola.poll(ESPERA_MAXIMA_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Registra que un pedido ya fue entregado.
     *
     * @param pedido pedido entregado
     */
    public synchronized void registrarEntrega(Pedido pedido) {
        entregados++;
    }

    /**
     * Obtiene los pedidos que ingresaron a la zona de carga.
     *
     * @return la lista de pedidos registrados, en orden de llegada
     */
    public synchronized List<Pedido> getPedidos() { return List.copyOf(registro); }

    /**
     * Obtiene la cantidad de pedidos que ingresaron a la zona de carga.
     *
     * @return el total de pedidos registrados
     */
    public synchronized int getTotalPedidos() { return registro.size(); }

    /**
     * Obtiene la cantidad de pedidos ya entregados.
     *
     * @return el total de entregas registradas
     */
    public synchronized int getEntregados() { return entregados; }

    /**
     * Indica si todos los pedidos de la zona de carga llegaron a su destino.
     *
     * @return {@code true} si cada pedido registrado quedó en estado
     *         {@link EstadoPedido#ENTREGADO} y se entregó exactamente una vez
     */
    public synchronized boolean todosEntregados() {
        if (entregados != registro.size()) return false;
        for (Pedido pedido : registro) {
            if (pedido.getEstado() != EstadoPedido.ENTREGADO) return false;
        }
        return true;
    }
}
