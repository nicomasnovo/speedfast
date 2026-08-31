package cl.duocuc.model;

/**
 * Pedido de encomienda, caracterizado por el peso y el volumen del paquete.
 */
public class PedidoEncomienda extends Pedido {
    private double peso;
    private double volumen;

    /**
     * Crea un pedido de encomienda.
     *
     * @param idPedido         identificador del pedido
     * @param direccionEntrega dirección donde se entrega el pedido
     * @param distanciaKm      distancia hasta el destino, en kilómetros
     * @param peso             peso del paquete, en kilogramos
     * @param volumen          volumen del paquete, en litros
     */
    public PedidoEncomienda(int idPedido, String direccionEntrega, double distanciaKm,
                            double peso, double volumen) {
        super(idPedido, direccionEntrega, distanciaKm);
        this.peso = peso;
        this.volumen = volumen;
    }

    /**
     * Obtiene el peso del paquete.
     *
     * @return el peso del paquete, en kilogramos
     */
    public double getPeso() { return peso; }

    /**
     * Obtiene el volumen del paquete.
     *
     * @return el volumen del paquete, en litros
     */
    public double getVolumen() { return volumen; }

    /**
     * Calcula el tiempo de entrega como 20 minutos base más 1,5 minutos
     * por kilómetro, truncado a un valor entero.
     *
     * @return el tiempo estimado de entrega, en minutos
     */
    @Override
    public int calcularTiempoEntrega() {
        return (int) (20 + 1.5 * getDistanciaKm());
    }

    /**
     * Asigna el repartidor de encomiendas por defecto e informa el peso
     * y el volumen del paquete.
     *
     * @return el mensaje con la asignación realizada
     */
    @Override
    public String asignarRepartidor() {
        return asignarRepartidor("Daniela Tapia")
            + " (peso: " + peso + " kg, volumen: " + volumen + " L)";
    }
}
