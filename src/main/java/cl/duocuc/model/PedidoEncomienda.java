package cl.duocuc.model;

public class PedidoEncomienda extends Pedido {
    private double peso;
    private double volumen;

    public PedidoEncomienda(String idPedido, String cliente, String direccion, double peso, double volumen) {
        super(idPedido, cliente, direccion);
        this.peso = peso;
        this.volumen = volumen;
    }

    public double getPeso() { return peso; }
    public double getVolumen() { return volumen; }

    @Override
    public String asignarRepartidor() {
        return "Repartidor de encomienda asignado al pedido " + getIdPedido()
            + " (peso: " + peso + " kg, volumen: " + volumen + " L)";
    }

    public String asignarRepartidor(boolean prioritario) {
        if (prioritario) {
            return "Repartidor PRIORITARIO de encomienda asignado al pedido " + getIdPedido()
                + " (peso: " + peso + " kg)";
        }
        return asignarRepartidor();
    }
}
