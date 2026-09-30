package cl.duocuc.model;

/**
 * Repartidor que retira pedidos de la {@link ZonaDeCarga} y los entrega dentro
 * de su propio hilo. Cada instancia es una tarea {@link Runnable}, por lo que
 * varios repartidores trabajan en paralelo sobre la misma zona de carga.
 */
public class Repartidor implements Runnable {

    /** Tiempo mínimo que demora una entrega, en milisegundos. */
    private static final long DEMORA_MINIMA_MS = 1000;

    /** Variación aleatoria que se suma a la demora mínima, en milisegundos. */
    private static final long DEMORA_VARIABLE_MS = 1500;

    /** Identificador del repartidor en la base de datos; 0 si aún no se ha guardado. */
    private int id;

    private String nombre;
    private final ZonaDeCarga zonaDeCarga;

    /** Entregas acumuladas; se escribe en el hilo del repartidor y se lee desde la interfaz. */
    private volatile int entregas;

    /**
     * Crea un repartidor sin identificador de base de datos.
     *
     * @param nombre      nombre del repartidor
     * @param zonaDeCarga zona de carga compartida desde la que retira los pedidos
     */
    public Repartidor(String nombre, ZonaDeCarga zonaDeCarga) {
        this(0, nombre, zonaDeCarga);
    }

    /**
     * Crea un repartidor asociado a una zona de carga.
     *
     * @param id          identificador del repartidor en la base de datos
     * @param nombre      nombre del repartidor
     * @param zonaDeCarga zona de carga compartida desde la que retira los pedidos
     */
    public Repartidor(int id, String nombre, ZonaDeCarga zonaDeCarga) {
        this.id = id;
        this.nombre = nombre;
        this.zonaDeCarga = zonaDeCarga;
    }

    /**
     * Obtiene el identificador del repartidor en la base de datos.
     *
     * @return el identificador del repartidor, o 0 si todavía no está guardado
     */
    public int getId() { return id; }

    /**
     * Asigna el identificador que la base de datos generó para el repartidor.
     *
     * @param id identificador del repartidor
     */
    public void setId(int id) { this.id = id; }

    /**
     * Obtiene el nombre del repartidor.
     *
     * @return el nombre del repartidor
     */
    public String getNombre() { return nombre; }

    /**
     * Cambia el nombre del repartidor.
     *
     * @param nombre nuevo nombre del repartidor
     */
    public void setNombre(String nombre) { this.nombre = nombre; }

    /**
     * Obtiene la cantidad de pedidos que el repartidor alcanzó a entregar.
     *
     * @return el total de entregas realizadas
     */
    public int getEntregas() { return entregas; }

    /**
     * Entrega la descripción del repartidor con su nombre y su identificador, de
     * modo que los componentes Swing (por ejemplo, el desplegable de repartidores
     * de la sección de entregas) la muestren directamente.
     *
     * @return la representación del repartidor en texto
     */
    @Override
    public String toString() {
        return nombre + " (ID " + id + ")";
    }

    /**
     * Cuerpo del hilo: retira pedidos de la zona de carga de uno en uno y los
     * entrega hasta que no queden pedidos disponibles.
     */
    @Override
    public void run() {
        try {
            Pedido pedido;
            while ((pedido = zonaDeCarga.retirarPedido(nombre)) != null) {
                pedido.setRepartidorAsignado(nombre);
                pedido.setEstado(EstadoPedido.EN_REPARTO);
                System.out.println("[Repartidor: " + nombre + "] Retirando pedido #"
                        + pedido.getId() + "...");
                entregar(pedido);
            }
            System.out.println("[Repartidor: " + nombre
                    + "] Sin pedidos pendientes en la zona de carga.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[Repartidor: " + nombre + "] Reparto interrumpido.");
        }
    }

    /**
     * Entrega un pedido ya retirado: simula el traslado con
     * {@link Thread#sleep(long)}, deja el pedido en estado
     * {@link EstadoPedido#ENTREGADO} y avisa la entrega a la zona de carga.
     *
     * @param pedido pedido en reparto
     * @throws InterruptedException si el hilo es interrumpido durante el traslado
     */
    private void entregar(Pedido pedido) throws InterruptedException {
        System.out.println("[Repartidor: " + nombre + "] Entregando pedido #"
                + pedido.getId() + "...");

        Thread.sleep(DEMORA_MINIMA_MS + (long) (Math.random() * DEMORA_VARIABLE_MS));

        pedido.setEstado(EstadoPedido.ENTREGADO);
        zonaDeCarga.registrarEntrega(pedido);
        entregas++;
        System.out.println("[Repartidor: " + nombre + "] Pedido #"
                + pedido.getId() + " entregado ✓");
    }
}
