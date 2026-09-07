package cl.duocuc.model;

/**
 * Pedido express despachado desde una tienda, con un tiempo de entrega reducido.
 */
public class PedidoExpress extends Pedido {
    private String tienda;

    /**
     * Crea un pedido express.
     *
     * @param idPedido         identificador del pedido
     * @param direccionEntrega dirección donde se entrega el pedido
     * @param distanciaKm      distancia hasta el destino, en kilómetros
     * @param tienda           nombre de la tienda desde donde se despacha
     */
    public PedidoExpress(int idPedido, String direccionEntrega, double distanciaKm, String tienda) {
        super(idPedido, direccionEntrega, distanciaKm);
        this.tienda = tienda;
    }

    /**
     * Obtiene la tienda desde donde se despacha el pedido.
     *
     * @return el nombre de la tienda desde donde se despacha el pedido
     */
    public String getTienda() { return tienda; }

    /**
     * Calcula el tiempo de entrega como 10 minutos base, con 5 minutos
     * adicionales cuando la distancia supera los 5 kilómetros.
     *
     * @return el tiempo estimado de entrega, en minutos
     */
    @Override
    public int calcularTiempoEntrega() {
        int tiempo = 10;
        if (getDistanciaKm() > 5) {
            tiempo += 5;
        }
        return tiempo;
    }

    /**
     * Asigna el repartidor express por defecto e informa la tienda de origen.
     *
     * @return el mensaje con la asignación realizada
     */
    @Override
    public String asignarRepartidor() {
        return asignarRepartidor("Camila Rojas") + " (despacha desde " + tienda + ")";
    }
}
