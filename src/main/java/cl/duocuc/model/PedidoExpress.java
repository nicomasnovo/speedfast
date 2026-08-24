package cl.duocuc.model;

public class PedidoExpress extends Pedido {
    private String tienda;

    public PedidoExpress(int idPedido, String direccionEntrega, double distanciaKm, String tienda) {
        super(idPedido, direccionEntrega, distanciaKm);
        this.tienda = tienda;
    }

    public String getTienda() { return tienda; }

    // Regla: 10 min base; si distancia > 5 km, se agregan 5 min extra
    @Override
    public int calcularTiempoEntrega() {
        int tiempo = 10;
        if (getDistanciaKm() > 5) {
            tiempo += 5;
        }
        return tiempo;
    }

    @Override
    public String asignarRepartidor() {
        return "Repartidor express asignado al pedido #" + String.format("%03d", getIdPedido())
            + " desde la tienda " + tienda + " (distancia: " + getDistanciaKm() + " km)";
    }

    public String asignarRepartidor(int horaLimite) {
        return "Repartidor express asignado al pedido #" + String.format("%03d", getIdPedido())
            + " desde " + tienda + " — debe entregar antes de las " + horaLimite + ":00 hrs";
    }
}
