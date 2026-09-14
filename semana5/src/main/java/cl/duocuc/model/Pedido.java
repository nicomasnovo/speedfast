package cl.duocuc.model;

/**
 * Pedido gestionado por SpeedFast. Guarda los datos de la entrega y el
 * {@link EstadoPedido} en que se encuentra.
 * <p>
 * El estado se lee y se escribe desde varios hilos (el repartidor que hace la
 * entrega y el hilo principal que revisa el resultado), por lo que los métodos
 * de acceso al estado están sincronizados.
 */
public class Pedido {

    private int id;
    private String direccionEntrega;
    private EstadoPedido estado;

    /**
     * Crea un pedido en estado {@link EstadoPedido#PENDIENTE}.
     *
     * @param id               identificador del pedido
     * @param direccionEntrega dirección donde se entrega el pedido
     */
    public Pedido(int id, String direccionEntrega) {
        this(id, direccionEntrega, EstadoPedido.PENDIENTE);
    }

    /**
     * Crea un pedido con un estado inicial determinado.
     *
     * @param id               identificador del pedido
     * @param direccionEntrega dirección donde se entrega el pedido
     * @param estado           estado inicial del pedido
     */
    public Pedido(int id, String direccionEntrega, EstadoPedido estado) {
        this.id = id;
        this.direccionEntrega = direccionEntrega;
        this.estado = estado;
    }

    /**
     * Obtiene el identificador del pedido.
     *
     * @return el identificador del pedido
     */
    public int getId() { return id; }

    /**
     * Asigna el identificador del pedido.
     *
     * @param id identificador del pedido
     */
    public void setId(int id) { this.id = id; }

    /**
     * Obtiene la dirección de entrega del pedido.
     *
     * @return la dirección de entrega del pedido
     */
    public String getDireccionEntrega() { return direccionEntrega; }

    /**
     * Asigna la dirección de entrega del pedido.
     *
     * @param direccionEntrega dirección donde se entrega el pedido
     */
    public void setDireccionEntrega(String direccionEntrega) {
        this.direccionEntrega = direccionEntrega;
    }

    /**
     * Obtiene el estado actual del pedido.
     *
     * @return el estado actual del pedido
     */
    public synchronized EstadoPedido getEstado() { return estado; }

    /**
     * Cambia el estado del pedido.
     *
     * @param estado nuevo estado del pedido
     */
    public synchronized void setEstado(EstadoPedido estado) { this.estado = estado; }

    /**
     * Cambia el estado del pedido a partir de su nombre, sin importar si viene
     * en mayúsculas o minúsculas (por ejemplo, {@code "en_reparto"}).
     *
     * @param estado nombre del estado, tal como se declara en {@link EstadoPedido}
     * @throws IllegalArgumentException si el nombre no corresponde a un estado válido
     */
    public synchronized void setEstado(String estado) {
        if (estado == null) {
            throw new IllegalArgumentException("El estado del pedido no puede ser nulo.");
        }
        this.estado = EstadoPedido.valueOf(estado.trim().toUpperCase());
    }

    /**
     * Entrega la descripción del pedido con su identificador, dirección y estado.
     *
     * @return la representación del pedido en texto
     */
    @Override
    public synchronized String toString() {
        return "Pedido #" + id + " - " + direccionEntrega + " [" + estado + "]";
    }
}
