package cl.duocuc.model;

/**
 * Tipos de pedido que despacha SpeedFast. Se mantienen los mismos tipos con que
 * trabaja la empresa desde las primeras versiones del sistema, ahora como enum
 * para que la vista pueda ofrecerlos sin escribir texto libre.
 */
public enum TipoPedido {

    /** Pedido de comida preparada. */
    COMIDA("Comida"),

    /** Encomienda o paquete. */
    ENCOMIENDA("Encomienda"),

    /** Pedido express del mismo día. */
    EXPRESS("Express");

    /** Nombre legible que se muestra en la interfaz. */
    private final String etiqueta;

    /**
     * Crea un tipo de pedido con su nombre legible.
     *
     * @param etiqueta nombre que se muestra al usuario
     */
    TipoPedido(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /**
     * Obtiene el nombre legible del tipo de pedido.
     *
     * @return la etiqueta del tipo de pedido
     */
    public String getEtiqueta() { return etiqueta; }

    /**
     * Entrega la etiqueta del tipo, de modo que los componentes Swing la muestren
     * directamente.
     *
     * @return la etiqueta del tipo de pedido
     */
    @Override
    public String toString() { return etiqueta; }
}
