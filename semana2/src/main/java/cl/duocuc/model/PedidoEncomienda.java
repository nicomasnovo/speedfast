package cl.duocuc.model;

public class PedidoEncomienda extends Pedido {
    private double peso;
    private double volumen;

    public PedidoEncomienda(int idPedido, String direccionEntrega, double distanciaKm,
                            double peso, double volumen) {
        super(idPedido, direccionEntrega, distanciaKm);
        this.peso = peso;
        this.volumen = volumen;
    }

    public double getPeso() { return peso; }
    public double getVolumen() { return volumen; }

    // Regla: 20 min + 1.5 min por kilómetro (ajustado a entero)
    @Override
    public int calcularTiempoEntrega() {
        return (int) (20 + 1.5 * getDistanciaKm());
    }

    @Override
    public String asignarRepartidor() {
        return "Repartidor de encomienda asignado al pedido #" + String.format("%03d", getIdPedido())
            + " (peso: " + peso + " kg, volumen: " + volumen + " L)";
    }

    public String asignarRepartidor(boolean prioritario) {
        if (prioritario) {
            return "Repartidor PRIORITARIO de encomienda asignado al pedido #" + String.format("%03d", getIdPedido())
                + " (peso: " + peso + " kg)";
        }
        return asignarRepartidor();
    }
}
