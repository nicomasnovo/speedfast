package cl.duocuc.model;

/**
 * Pedido de comida preparada en un restaurante.
 * Su tiempo de entrega considera el trayecto desde el local hasta el cliente.
 */
public class PedidoComida extends Pedido {
    private String restaurante;
    private int tiempoPreparacion;

    /**
     * Crea un pedido de comida.
     *
     * @param idPedido          identificador del pedido
     * @param direccionEntrega  dirección donde se entrega el pedido
     * @param distanciaKm       distancia hasta el destino, en kilómetros
     * @param restaurante       nombre del restaurante que prepara el pedido
     * @param tiempoPreparacion tiempo de preparación en el local, en minutos
     */
    public PedidoComida(int idPedido, String direccionEntrega, double distanciaKm,
                        String restaurante, int tiempoPreparacion) {
        super(idPedido, direccionEntrega, distanciaKm);
        this.restaurante = restaurante;
        this.tiempoPreparacion = tiempoPreparacion;
    }

    /**
     * Obtiene el restaurante que prepara el pedido.
     *
     * @return el nombre del restaurante que prepara el pedido
     */
    public String getRestaurante() { return restaurante; }

    /**
     * Obtiene el tiempo de preparación del pedido en el local.
     *
     * @return el tiempo de preparación en el local, en minutos
     */
    public int getTiempoPreparacion() { return tiempoPreparacion; }

    /**
     * Calcula el tiempo de entrega como 15 minutos base más 2 minutos
     * por cada kilómetro de distancia.
     *
     * @return el tiempo estimado de entrega, en minutos
     */
    @Override
    public int calcularTiempoEntrega() {
        return 15 + (int) (2 * getDistanciaKm());
    }

    /**
     * Asigna el repartidor de comida por defecto e informa el restaurante
     * de origen junto con su tiempo de preparación.
     *
     * @return el mensaje con la asignación realizada
     */
    @Override
    public String asignarRepartidor() {
        return asignarRepartidor("Luis Díaz")
            + " (retira en " + restaurante + ", preparación: " + tiempoPreparacion + " min)";
    }
}
