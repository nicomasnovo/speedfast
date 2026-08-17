package cl.duocuc.model;

public class PedidoComida extends Pedido {
    private String restaurante;
    private int tiempoPreparacion;

    public PedidoComida(String idPedido, String cliente, String direccion, String restaurante, int tiempoPreparacion) {
        super(idPedido, cliente, direccion);
        this.restaurante = restaurante;
        this.tiempoPreparacion = tiempoPreparacion;
    }

    public String getRestaurante() { return restaurante; }
    public int getTiempoPreparacion() { return tiempoPreparacion; }

    @Override
    public String asignarRepartidor() {
        return "Repartidor de comida asignado al pedido " + getIdPedido()
            + " del restaurante " + restaurante
            + " (preparación: " + tiempoPreparacion + " min)";
    }

    public String asignarRepartidor(boolean urgente) {
        if (urgente) {
            return "Repartidor URGENTE de comida asignado al pedido " + getIdPedido()
                + " del restaurante " + restaurante;
        }
        return asignarRepartidor();
    }
}
