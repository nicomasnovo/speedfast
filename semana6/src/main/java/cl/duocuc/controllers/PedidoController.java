package cl.duocuc.controllers;

import cl.duocuc.model.EstadoPedido;
import cl.duocuc.model.Pedido;
import cl.duocuc.model.TipoPedido;
import cl.duocuc.model.ZonaDeCarga;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de pedidos: recibe las solicitudes de la vista, valida los datos y
 * los registra o consulta en la {@link ZonaDeCarga}, que es el almacenamiento en
 * memoria del sistema.
 * <p>
 * Todas las vistas deben compartir la misma instancia de este controlador para
 * trabajar sobre los mismos pedidos.
 */
public class PedidoController {

    private final ZonaDeCarga zonaDeCarga;

    /**
     * Crea el controlador sobre una zona de carga existente.
     *
     * @param zonaDeCarga zona de carga compartida donde viven los pedidos
     */
    public PedidoController(ZonaDeCarga zonaDeCarga) {
        this.zonaDeCarga = zonaDeCarga;
    }

    /**
     * Registra un pedido a partir de los datos escritos en la vista.
     *
     * @param idTexto   identificador del pedido tal como lo escribió el usuario
     * @param direccion dirección de entrega
     * @param tipo      tipo de pedido seleccionado
     * @return el pedido registrado
     * @throws IllegalArgumentException si algún dato no es válido o el identificador ya existe
     */
    public Pedido registrarPedido(String idTexto, String direccion, TipoPedido tipo) {
        if (idTexto == null || idTexto.isBlank()) {
            throw new IllegalArgumentException("Debe indicar el ID del pedido.");
        }
        int id;
        try {
            id = Integer.parseInt(idTexto.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El ID del pedido debe ser un número entero.");
        }
        return registrarPedido(id, direccion, tipo);
    }

    /**
     * Registra un pedido con los datos ya convertidos.
     *
     * @param id        identificador del pedido
     * @param direccion dirección de entrega
     * @param tipo      tipo de pedido
     * @return el pedido registrado
     * @throws IllegalArgumentException si algún dato no es válido o el identificador ya existe
     */
    public Pedido registrarPedido(int id, String direccion, TipoPedido tipo) {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID del pedido debe ser mayor que cero.");
        }
        if (direccion == null || direccion.isBlank()) {
            throw new IllegalArgumentException("Debe indicar la dirección de entrega.");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Debe seleccionar el tipo de pedido.");
        }
        if (zonaDeCarga.buscarPedido(id) != null) {
            throw new IllegalArgumentException("Ya existe un pedido con el ID " + id + ".");
        }

        Pedido pedido = new Pedido(id, direccion.trim(), tipo);
        zonaDeCarga.agregarPedido(pedido);
        return pedido;
    }

    /**
     * Obtiene todos los pedidos registrados, en orden de llegada.
     *
     * @return la lista de pedidos del sistema
     */
    public List<Pedido> getPedidos() { return zonaDeCarga.getPedidos(); }

    /**
     * Obtiene los pedidos que todavía están esperando reparto.
     *
     * @return la lista de pedidos en estado {@link EstadoPedido#PENDIENTE}
     */
    public List<Pedido> getPedidosPendientes() {
        List<Pedido> pendientes = new ArrayList<>();
        for (Pedido pedido : zonaDeCarga.getPedidos()) {
            if (pedido.getEstado() == EstadoPedido.PENDIENTE) pendientes.add(pedido);
        }
        return pendientes;
    }

    /**
     * Obtiene los tipos de pedido que puede ofrecer la vista.
     *
     * @return los valores del enum {@link TipoPedido}
     */
    public TipoPedido[] getTiposDePedido() { return TipoPedido.values(); }

    /**
     * Propone el siguiente identificador libre, para facilitar el registro.
     *
     * @return el mayor identificador registrado más uno, o 101 si no hay pedidos
     */
    public int sugerirId() {
        int mayor = 100;
        for (Pedido pedido : zonaDeCarga.getPedidos()) {
            if (pedido.getId() > mayor) mayor = pedido.getId();
        }
        return mayor + 1;
    }

    /**
     * Obtiene la cantidad total de pedidos registrados.
     *
     * @return el total de pedidos
     */
    public int getTotalPedidos() { return zonaDeCarga.getTotalPedidos(); }

    /**
     * Obtiene la cantidad de pedidos ya entregados.
     *
     * @return el total de entregas registradas
     */
    public int getEntregados() { return zonaDeCarga.getEntregados(); }

    /**
     * Indica si todos los pedidos registrados llegaron a su destino.
     *
     * @return {@code true} si no quedan pedidos por entregar
     */
    public boolean todosEntregados() { return zonaDeCarga.todosEntregados(); }
}
