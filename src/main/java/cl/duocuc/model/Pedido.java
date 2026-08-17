package cl.duocuc.model;

public class Pedido {
    private String idPedido;
    private String cliente;
    private String direccion;

    public Pedido(String idPedido, String cliente, String direccion) {
        this.idPedido = idPedido;
        this.cliente = cliente;
        this.direccion = direccion;
    }

    public String getIdPedido() { return idPedido; }
    public String getCliente()  { return cliente; }
    public String getDireccion() { return direccion; }

    public String asignarRepartidor() {
        return "Repartidor estándar asignado al pedido " + idPedido;
    }

    public String asignarRepartidor(String tipo) {
        return "Repartidor de tipo '" + tipo + "' asignado al pedido " + idPedido;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [id=" + idPedido + ", cliente=" + cliente + ", dirección=" + direccion + "]";
    }
}
