package cl.duocuc.model;

public class PedidoComida extends Pedido {
    private String restaurante;
    private int tiempoPreparacion;

    public PedidoComida(int idPedido, String direccionEntrega, double distanciaKm,
                        String restaurante, int tiempoPreparacion) {
        super(idPedido, direccionEntrega, distanciaKm);
        this.restaurante = restaurante;
        this.tiempoPreparacion = tiempoPreparacion;
    }

    public String getRestaurante() { return restaurante; }
    public int getTiempoPreparacion() { return tiempoPreparacion; }

    // Regla: 15 min + 2 min por cada kilómetro
    @Override
    public int calcularTiempoEntrega() {
        return 15 + (int) (2 * getDistanciaKm());
    }

    @Override
    public String asignarRepartidor() {
        return "Repartidor de comida asignado al pedido #" + String.format("%03d", getIdPedido())
            + " del restaurante " + restaurante
            + " (preparación: " + tiempoPreparacion + " min)";
    }

    public String asignarRepartidor(boolean urgente) {
        if (urgente) {
            return "Repartidor URGENTE de comida asignado al pedido #" + String.format("%03d", getIdPedido())
                + " del restaurante " + restaurante;
        }
        return asignarRepartidor();
    }
}
