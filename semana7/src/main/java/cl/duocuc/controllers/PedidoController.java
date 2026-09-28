package cl.duocuc.controllers;

import cl.duocuc.dao.PedidoDAO;
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
 * Los pedidos además se guardan en MySQL, pero este controlador ya no habla
 * directamente con la base de datos: delega toda la persistencia en
 * {@link PedidoDAO} y se queda con la coordinación (validar los datos de la
 * vista, crear los objetos {@link Pedido} y mantener la zona de carga al día).
 * El flujo es vista → controlador → DAO → ConexionBD → MySQL.
 * <p>
 * Todas las vistas deben compartir la misma instancia de este controlador para
 * trabajar sobre los mismos pedidos.
 */
public class PedidoController {

    private final ZonaDeCarga zonaDeCarga;
    private final PedidoDAO pedidoDAO;

    /**
     * Crea el controlador sobre una zona de carga existente.
     *
     * @param zonaDeCarga zona de carga compartida donde viven los pedidos
     */
    public PedidoController(ZonaDeCarga zonaDeCarga) {
        this.zonaDeCarga = zonaDeCarga;
        this.pedidoDAO = new PedidoDAO();
    }

    /**
     * Registra un pedido a partir de los datos escritos en la vista.
     *
     * @param idTexto   identificador del pedido tal como lo escribió el usuario
     * @param direccion dirección de entrega
     * @param tipo      tipo de pedido seleccionado
     * @return el pedido registrado, o {@code null} si no se pudo guardar en la
     *         base de datos (en ese caso el error ya se informó al usuario)
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
     * Registra un pedido con los datos ya convertidos: primero lo guarda en MySQL
     * y solo entonces lo deja disponible en la zona de carga, para que la lista en
     * memoria y la base de datos no queden descuadradas.
     *
     * @param id        identificador del pedido
     * @param direccion dirección de entrega
     * @param tipo      tipo de pedido
     * @return el pedido registrado, o {@code null} si no se pudo guardar en la
     *         base de datos (en ese caso el error ya se informó al usuario)
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
        if (!agregarPedido(pedido)) {
            return null;
        }
        zonaDeCarga.agregarPedido(pedido);
        return pedido;
    }

    /**
     * Pide al DAO que guarde un pedido en la base de datos.
     *
     * @param pedido pedido que se quiere insertar
     * @return {@code true} si el pedido quedó guardado
     */
    public boolean agregarPedido(Pedido pedido) {
        return pedidoDAO.agregar(pedido);
    }

    /**
     * Pide al DAO que actualice la dirección, el tipo, el estado y el repartidor
     * de un pedido ya existente. También se usa para guardar la asignación hecha
     * desde la interfaz, porque esa asignación solo cambia el repartidor del
     * pedido.
     *
     * @param pedido pedido con los datos nuevos; su identificador indica la fila
     * @return {@code true} si se actualizó alguna fila
     */
    public boolean editarPedido(Pedido pedido) {
        return pedidoDAO.editar(pedido);
    }

    /**
     * Elimina un pedido de la base de datos y, si lo logra, también de la zona de
     * carga, para que la lista en memoria refleje lo mismo que MySQL.
     *
     * @param id identificador del pedido que se quiere eliminar
     * @return {@code true} si el pedido fue eliminado
     */
    public boolean eliminarPedido(int id) {
        if (!pedidoDAO.eliminar(id)) {
            return false;
        }

        Pedido pedido = zonaDeCarga.buscarPedido(id);
        if (pedido != null) {
            zonaDeCarga.quitarPedido(pedido);
        }
        return true;
    }

    /**
     * Lee los pedidos guardados en MySQL y rearma con ellos la zona de carga.
     * <p>
     * La zona de carga se limpia antes de cargar, por lo que el método se puede
     * llamar todas las veces que se quiera sin duplicar pedidos.
     *
     * @return la cantidad de pedidos cargados desde la base de datos
     */
    public int cargarPedidosDesdeDB() {
        List<Pedido> leidos = pedidoDAO.cargarDesdeDB();

        // El DAO devuelve null si la lectura falló y ya avisó del error: la zona
        // de carga se deja intacta en vez de vaciarla.
        if (leidos == null) {
            return 0;
        }

        zonaDeCarga.limpiar();
        for (Pedido pedido : leidos) {
            zonaDeCarga.reponerPedido(pedido);
        }
        return leidos.size();
    }

    /**
     * Guarda en la base de datos el estado y el repartidor actuales de todos los
     * pedidos en memoria. Se usa al terminar el reparto, para que las entregas
     * hechas por los hilos de los repartidores queden persistidas junto con el
     * repartidor que se hizo cargo de cada pedido.
     *
     * @return la cantidad de pedidos actualizados
     */
    public int guardarEstados() {
        List<Pedido> pedidos = zonaDeCarga.getPedidos();
        if (pedidos.isEmpty()) {
            return 0;
        }

        return pedidoDAO.guardarEstados(pedidos);
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
