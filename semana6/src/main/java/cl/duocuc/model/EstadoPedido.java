package cl.duocuc.model;

/**
 * Estados por los que pasa un pedido durante el despacho.
 * Al usar un enum en lugar de texto libre se evitan errores de tipeo y el
 * estado de cada pedido queda limitado a los valores válidos del proceso.
 */
public enum EstadoPedido {

    /** El pedido está en la zona de carga esperando a un repartidor. */
    PENDIENTE,

    /** Un repartidor retiró el pedido y está en camino al destino. */
    EN_REPARTO,

    /** El pedido ya fue entregado al destinatario. */
    ENTREGADO
}
