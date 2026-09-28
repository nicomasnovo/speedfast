package cl.duocuc.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Zona de carga de SpeedFast: el almacenamiento en memoria de los pedidos y el
 * recurso compartido desde el que los repartidores los retiran.
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
    private final Map<String, BlockingQueue<Pedido>> colasAsignadas = new ConcurrentHashMap<>();
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
     * Repone en la zona de carga un pedido que ya existía en la base de datos,
     * respetando el estado con que fue guardado: solo los pedidos pendientes
     * quedan disponibles para los repartidores y los entregados se suman al
     * contador de entregas.
     *
     * @param pedido pedido leído desde la base de datos
     */
    public synchronized void reponerPedido(Pedido pedido) {
        registro.add(pedido);
        if (pedido.getEstado() == EstadoPedido.PENDIENTE) {
            cola.add(pedido);
        } else if (pedido.getEstado() == EstadoPedido.ENTREGADO) {
            entregados++;
        }
    }

    /**
     * Saca un pedido del registro y de las colas de reparto.
     *
     * @param pedido pedido que se quiere quitar de la zona de carga
     * @return {@code true} si el pedido estaba registrado
     */
    public synchronized boolean quitarPedido(Pedido pedido) {
        quitarDeLasColas(pedido);
        return registro.remove(pedido);
    }

    /**
     * Deja la zona de carga vacía. Se usa antes de volver a leer los pedidos
     * desde la base de datos, para no duplicar registros.
     */
    public synchronized void limpiar() {
        cola.clear();
        colasAsignadas.clear();
        registro.clear();
        entregados = 0;
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
     * Retira un pedido para un repartidor concreto: primero los que le fueron
     * asignados y, si no tiene ninguno, el siguiente de la cola general. Cada
     * pedido vive en una sola cola, por lo que el retiro sigue siendo atómico.
     *
     * @param nombreRepartidor nombre del repartidor que retira el pedido
     * @return el siguiente pedido disponible para ese repartidor, o {@code null}
     *         si no queda ninguno tras esperar por la cola general
     * @throws InterruptedException si el hilo es interrumpido mientras espera un pedido
     */
    public Pedido retirarPedido(String nombreRepartidor) throws InterruptedException {
        BlockingQueue<Pedido> colaPropia = colasAsignadas.get(nombreRepartidor);
        if (colaPropia != null) {
            Pedido asignado = colaPropia.poll();
            if (asignado != null) return asignado;
        }
        return retirarPedido();
    }

    /**
     * Reserva un pedido para un repartidor: lo saca de la cola en que esté y lo
     * deja en la cola propia de ese repartidor, de modo que sea él quien lo
     * entregue cuando comience el reparto.
     *
     * @param pedido           pedido que se quiere asignar
     * @param nombreRepartidor nombre del repartidor que se hará cargo
     * @throws IllegalStateException si el pedido ya fue retirado por un repartidor
     *                               o ya no está pendiente
     */
    public synchronized void asignarRepartidor(Pedido pedido, String nombreRepartidor) {
        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new IllegalStateException("El pedido #" + pedido.getId()
                    + " ya no está pendiente: " + pedido.getEstado() + ".");
        }
        if (!quitarDeLasColas(pedido)) {
            throw new IllegalStateException("El pedido #" + pedido.getId()
                    + " ya fue retirado por un repartidor.");
        }
        pedido.setRepartidorAsignado(nombreRepartidor);
        colasAsignadas.computeIfAbsent(nombreRepartidor, nombre -> new LinkedBlockingQueue<>())
                .add(pedido);
        System.out.println("[ZonaDeCarga] Pedido #" + pedido.getId()
                + " asignado a " + nombreRepartidor);
    }

    /**
     * Saca un pedido de la cola general o de la cola propia en que se encuentre.
     *
     * @param pedido pedido que se quiere quitar
     * @return {@code true} si el pedido seguía disponible y se pudo quitar
     */
    private boolean quitarDeLasColas(Pedido pedido) {
        if (cola.remove(pedido)) return true;
        for (BlockingQueue<Pedido> colaPropia : colasAsignadas.values()) {
            if (colaPropia.remove(pedido)) return true;
        }
        return false;
    }

    /**
     * Busca un pedido registrado por su identificador.
     *
     * @param id identificador del pedido
     * @return el pedido con ese identificador, o {@code null} si no existe
     */
    public synchronized Pedido buscarPedido(int id) {
        for (Pedido pedido : registro) {
            if (pedido.getId() == id) return pedido;
        }
        return null;
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
