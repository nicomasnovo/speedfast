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
    private TipoPedido tipo;
    private EstadoPedido estado;
    private String repartidorAsignado;

    /**
     * Crea un pedido de tipo {@link TipoPedido#COMIDA} en estado
     * {@link EstadoPedido#PENDIENTE}.
     *
     * @param id               identificador del pedido
     * @param direccionEntrega dirección donde se entrega el pedido
     */
    public Pedido(int id, String direccionEntrega) {
        this(id, direccionEntrega, TipoPedido.COMIDA);
    }

    /**
     * Crea un pedido en estado {@link EstadoPedido#PENDIENTE}.
     *
     * @param id               identificador del pedido
     * @param direccionEntrega dirección donde se entrega el pedido
     * @param tipo             tipo de pedido
     */
    public Pedido(int id, String direccionEntrega, TipoPedido tipo) {
        this(id, direccionEntrega, tipo, EstadoPedido.PENDIENTE);
    }

    /**
     * Crea un pedido con un estado inicial determinado.
     *
     * @param id               identificador del pedido
     * @param direccionEntrega dirección donde se entrega el pedido
     * @param tipo             tipo de pedido
     * @param estado           estado inicial del pedido
     */
    public Pedido(int id, String direccionEntrega, TipoPedido tipo, EstadoPedido estado) {
        this.id = id;
        this.direccionEntrega = direccionEntrega;
        this.tipo = tipo;
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
     * Obtiene el tipo del pedido.
     *
     * @return el tipo del pedido
     */
    public TipoPedido getTipo() { return tipo; }

    /**
     * Asigna el tipo del pedido.
     *
     * @param tipo tipo del pedido
     */
    public void setTipo(TipoPedido tipo) { this.tipo = tipo; }

    /**
     * Obtiene el nombre del repartidor asignado al pedido.
     * <p>
     * La asignación la escribe el hilo del repartidor que retira el pedido (o el
     * controlador, cuando se asigna manualmente) y la lee la interfaz, por lo que
     * el acceso está sincronizado.
     *
     * @return el nombre del repartidor, o {@code null} si el pedido aún no tiene uno
     */
    public synchronized String getRepartidorAsignado() { return repartidorAsignado; }

    /**
     * Asigna el repartidor responsable del pedido.
     *
     * @param repartidorAsignado nombre del repartidor
     */
    public synchronized void setRepartidorAsignado(String repartidorAsignado) {
        this.repartidorAsignado = repartidorAsignado;
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
     * Entrega la descripción del pedido con su identificador, dirección, tipo,
     * estado y, si lo tiene, el repartidor asignado.
     *
     * @return la representación del pedido en texto
     */
    @Override
    public synchronized String toString() {
        String descripcion = "Pedido #" + id + " - " + direccionEntrega
                + " (" + tipo + ") [" + estado + "]";
        if (repartidorAsignado != null) {
            descripcion += " - " + repartidorAsignado;
        }
        return descripcion;
    }
}
