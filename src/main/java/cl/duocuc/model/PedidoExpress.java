package cl.duocuc.model;

public class PedidoExpress extends Pedido {
    private String tienda;
    private double distancia;

    public PedidoExpress(String idPedido, String cliente, String direccion, String tienda, double distancia) {
        super(idPedido, cliente, direccion);
        this.tienda = tienda;
        this.distancia = distancia;
    }

    public String getTienda() { return tienda; }
    public double getDistancia() { return distancia; }

    @Override
    public String asignarRepartidor() {
        return "Repartidor express asignado al pedido " + getIdPedido()
            + " desde la tienda " + tienda + " (distancia: " + distancia + " km)";
    }

    public String asignarRepartidor(int horaLimite) {
        return "Repartidor express asignado al pedido " + getIdPedido()
            + " desde " + tienda + " — debe entregar antes de las " + horaLimite + ":00 hrs";
    }
}
