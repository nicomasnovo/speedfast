package cl.duocuc.model;

public abstract class Pedido {
    private int idPedido;
    private String direccionEntrega;
    private double distanciaKm;

    public Pedido(int idPedido, String direccionEntrega, double distanciaKm) {
        this.idPedido = idPedido;
        this.direccionEntrega = direccionEntrega;
        this.distanciaKm = distanciaKm;
    }

    public int getIdPedido() { return idPedido; }
    public String getDireccionEntrega() { return direccionEntrega; }
    public double getDistanciaKm() { return distanciaKm; }

    public void mostrarResumen() {
        System.out.println(getClass().getSimpleName() + " #" + String.format("%03d", idPedido));
        System.out.println("Dirección: " + direccionEntrega);
        System.out.println("Distancia: " + String.format("%.0f", distanciaKm) + " km");
        System.out.println("Tiempo estimado de entrega: " + calcularTiempoEntrega() + " minutos");
    }

    public abstract int calcularTiempoEntrega();

    public String asignarRepartidor() {
        return "Repartidor estándar asignado al pedido #" + String.format("%03d", idPedido);
    }

    public String asignarRepartidor(String tipo) {
        return "Repartidor de tipo '" + tipo + "' asignado al pedido #" + String.format("%03d", idPedido);
    }
}
