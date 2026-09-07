package cl.duocuc.model;

/**
 * Clase base de todos los pedidos gestionados por SpeedFast.
 * Define los datos comunes de una entrega y delega en cada subclase
 * el cálculo del tiempo estimado de entrega.
 */
public abstract class Pedido {
    private int idPedido;
    private String direccionEntrega;
    private double distanciaKm;
    private String repartidor;

    /**
     * Crea un pedido con sus datos básicos de entrega.
     *
     * @param idPedido         identificador del pedido
     * @param direccionEntrega dirección donde se entrega el pedido
     * @param distanciaKm      distancia hasta el destino, en kilómetros
     */
    public Pedido(int idPedido, String direccionEntrega, double distanciaKm) {
        this.idPedido = idPedido;
        this.direccionEntrega = direccionEntrega;
        this.distanciaKm = distanciaKm;
    }

    /**
     * Obtiene el identificador del pedido.
     *
     * @return el identificador del pedido
     */
    public int getIdPedido() { return idPedido; }

    /**
     * Obtiene la dirección de entrega del pedido.
     *
     * @return la dirección de entrega del pedido
     */
    public String getDireccionEntrega() { return direccionEntrega; }

    /**
     * Obtiene la distancia hasta el destino.
     *
     * @return la distancia hasta el destino, en kilómetros
     */
    public double getDistanciaKm() { return distanciaKm; }

    /**
     * Obtiene el repartidor asignado al pedido.
     *
     * @return el nombre del repartidor asignado, o {@code null} si aún no tiene uno
     */
    public String getRepartidor() { return repartidor; }

    /**
     * Imprime en consola el resumen del pedido: tipo, identificador,
     * dirección, distancia y tiempo estimado de entrega.
     */
    public void mostrarResumen() {
        System.out.println(getClass().getSimpleName() + " #" + String.format("%03d", idPedido));
        System.out.println("Dirección: " + direccionEntrega);
        System.out.println("Distancia: " + String.format("%.0f", distanciaKm) + " km");
        System.out.println("Tiempo estimado de entrega: " + calcularTiempoEntrega() + " minutos");
    }

    /**
     * Calcula el tiempo estimado de entrega según las reglas de cada tipo de pedido.
     *
     * @return el tiempo estimado de entrega, en minutos
     */
    public abstract int calcularTiempoEntrega();

    /**
     * Asigna al pedido el repartidor por defecto de su tipo.
     *
     * @return el mensaje con la asignación realizada
     */
    public String asignarRepartidor() {
        return asignarRepartidor("Repartidor estándar");
    }

    /**
     * Asigna al pedido el repartidor indicado por su nombre.
     *
     * @param nombre nombre del repartidor a cargo de la entrega
     * @return el mensaje con la asignación realizada
     */
    public String asignarRepartidor(String nombre) {
        this.repartidor = nombre;
        return "Repartidor asignado al pedido #" + String.format("%03d", idPedido) + ": " + nombre;
    }
}
