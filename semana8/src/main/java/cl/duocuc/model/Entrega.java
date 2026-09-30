package cl.duocuc.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Entrega registrada en la tabla {@code entrega}: deja constancia de qué
 * repartidor se hizo cargo de qué pedido, y en qué fecha y hora.
 * <p>
 * La tabla es independiente de {@code pedido}, por lo que la entrega guarda los
 * identificadores del pedido y del repartidor ({@code id_pedido} e
 * {@code id_repartidor}, las llaves foráneas). Además guarda la dirección del
 * pedido y el nombre del repartidor, que el DAO trae con un JOIN para que la
 * tabla de la interfaz pueda mostrar datos entendibles en lugar de solo números.
 * Esos dos campos son solo de lectura: no se guardan en la tabla {@code entrega}.
 */
public class Entrega {

    /** Identificador de la entrega; 0 si aún no se ha guardado. */
    private int id;

    private int idPedido;
    private int idRepartidor;
    private LocalDate fecha;
    private LocalTime hora;

    /** Dirección del pedido, leída con un JOIN solo para mostrarla. */
    private String direccionPedido;

    /** Nombre del repartidor, leído con un JOIN solo para mostrarlo. */
    private String nombreRepartidor;

    /**
     * Crea una entrega nueva, sin identificador de base de datos.
     *
     * @param idPedido     identificador del pedido entregado
     * @param idRepartidor identificador del repartidor que la realizó
     * @param fecha        fecha de la entrega
     * @param hora         hora de la entrega
     */
    public Entrega(int idPedido, int idRepartidor, LocalDate fecha, LocalTime hora) {
        this(0, idPedido, idRepartidor, fecha, hora);
    }

    /**
     * Crea una entrega con su identificador de base de datos.
     *
     * @param id           identificador de la entrega
     * @param idPedido     identificador del pedido entregado
     * @param idRepartidor identificador del repartidor que la realizó
     * @param fecha        fecha de la entrega
     * @param hora         hora de la entrega
     */
    public Entrega(int id, int idPedido, int idRepartidor, LocalDate fecha, LocalTime hora) {
        this.id = id;
        this.idPedido = idPedido;
        this.idRepartidor = idRepartidor;
        this.fecha = fecha;
        this.hora = hora;
    }

    /**
     * Obtiene el identificador de la entrega.
     *
     * @return el identificador de la entrega, o 0 si todavía no está guardada
     */
    public int getId() { return id; }

    /**
     * Asigna el identificador que la base de datos generó para la entrega.
     *
     * @param id identificador de la entrega
     */
    public void setId(int id) { this.id = id; }

    /**
     * Obtiene el identificador del pedido entregado.
     *
     * @return el identificador del pedido
     */
    public int getIdPedido() { return idPedido; }

    /**
     * Asigna el pedido de la entrega.
     *
     * @param idPedido identificador del pedido
     */
    public void setIdPedido(int idPedido) { this.idPedido = idPedido; }

    /**
     * Obtiene el identificador del repartidor que realizó la entrega.
     *
     * @return el identificador del repartidor
     */
    public int getIdRepartidor() { return idRepartidor; }

    /**
     * Asigna el repartidor de la entrega.
     *
     * @param idRepartidor identificador del repartidor
     */
    public void setIdRepartidor(int idRepartidor) { this.idRepartidor = idRepartidor; }

    /**
     * Obtiene la fecha de la entrega.
     *
     * @return la fecha de la entrega
     */
    public LocalDate getFecha() { return fecha; }

    /**
     * Asigna la fecha de la entrega.
     *
     * @param fecha fecha de la entrega
     */
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    /**
     * Obtiene la hora de la entrega.
     *
     * @return la hora de la entrega
     */
    public LocalTime getHora() { return hora; }

    /**
     * Asigna la hora de la entrega.
     *
     * @param hora hora de la entrega
     */
    public void setHora(LocalTime hora) { this.hora = hora; }

    /**
     * Obtiene la dirección del pedido entregado, para mostrarla en la interfaz.
     *
     * @return la dirección del pedido, o {@code null} si no se leyó con JOIN
     */
    public String getDireccionPedido() { return direccionPedido; }

    /**
     * Guarda la dirección del pedido leída con el JOIN del DAO.
     *
     * @param direccionPedido dirección del pedido entregado
     */
    public void setDireccionPedido(String direccionPedido) {
        this.direccionPedido = direccionPedido;
    }

    /**
     * Obtiene el nombre del repartidor, para mostrarlo en la interfaz.
     *
     * @return el nombre del repartidor, o {@code null} si no se leyó con JOIN
     */
    public String getNombreRepartidor() { return nombreRepartidor; }

    /**
     * Guarda el nombre del repartidor leído con el JOIN del DAO.
     *
     * @param nombreRepartidor nombre del repartidor que realizó la entrega
     */
    public void setNombreRepartidor(String nombreRepartidor) {
        this.nombreRepartidor = nombreRepartidor;
    }

    /**
     * Entrega la descripción de la entrega con el pedido, el repartidor, la fecha
     * y la hora.
     *
     * @return la representación de la entrega en texto
     */
    @Override
    public String toString() {
        return "Entrega #" + id + " - Pedido #" + idPedido
                + (direccionPedido != null ? " (" + direccionPedido + ")" : "")
                + " - " + (nombreRepartidor != null ? nombreRepartidor : "Repartidor " + idRepartidor)
                + " - " + fecha + " " + hora;
    }
}
