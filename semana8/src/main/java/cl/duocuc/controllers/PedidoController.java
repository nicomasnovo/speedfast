package cl.duocuc.controllers;

import cl.duocuc.dao.EntregaDAO;
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
     * Solo se consulta para saber cuántas entregas dependen de un pedido antes de
     * eliminarlo. Las entregas las gestiona {@link EntregaController}.
     */
    private final EntregaDAO entregaDAO;

    /**
     * Crea el controlador sobre una zona de carga existente.
     *
     * @param zonaDeCarga zona de carga compartida donde viven los pedidos
     */
    public PedidoController(ZonaDeCarga zonaDeCarga) {
        this.zonaDeCarga = zonaDeCarga;
        this.pedidoDAO = new PedidoDAO();
        this.entregaDAO = new EntregaDAO();
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
        if (pedidoDAO.buscarPorId(id) != null) {
            throw new IllegalArgumentException(
                    "Ya existe un pedido con el ID " + id + " en la base de datos.");
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
     * Cierra un pedido como entregado por un repartidor y guarda el cambio en
     * MySQL. Lo usa {@link EntregaController} cuando el usuario registra una
     * entrega: una entrega no solo asigna el pedido, lo deja entregado.
     * <p>
     * Si el pedido ya figuraba como entregado solo se actualiza quién lo entregó,
     * de modo que corregir la entrega no vuelva a sumarla al total.
     *
     * @param idPedido         identificador del pedido entregado
     * @param nombreRepartidor nombre del repartidor que lo entregó
     * @return {@code true} si el pedido quedó actualizado en la base de datos
     * @throws IllegalArgumentException si el pedido no está en la zona de carga o
     *                                  no se indica el repartidor
     * @throws IllegalStateException    si el pedido está en reparto en ese momento
     */
    public boolean marcarEntregado(int idPedido, String nombreRepartidor) {
        Pedido pedido = zonaDeCarga.buscarPedido(idPedido);
        if (pedido == null) {
            throw new IllegalArgumentException("No existe un pedido con el ID " + idPedido + ".");
        }
        if (nombreRepartidor == null || nombreRepartidor.isBlank()) {
            throw new IllegalArgumentException("Debe indicar el repartidor que entregó el pedido.");
        }

        if (pedido.getEstado() == EstadoPedido.ENTREGADO) {
            pedido.setRepartidorAsignado(nombreRepartidor);
        } else {
            zonaDeCarga.marcarEntregado(pedido, nombreRepartidor);
        }
        return editarPedido(pedido);
    }

    /**
     * Edita un pedido con los datos que llegan desde el formulario de la vista:
     * valida, guarda los cambios en MySQL y recarga la zona de carga para que la
     * lista en memoria quede igual que la base de datos.
     * <p>
     * Las reglas de negocio viven aquí, no en el DAO:
     * <ul>
     *   <li>la dirección, el tipo y el estado son obligatorios;</li>
     *   <li>un pedido sin repartidor asignado no puede quedar en
     *       {@link EstadoPedido#EN_REPARTO} ni {@link EstadoPedido#ENTREGADO},
     *       porque alguien tiene que hacerse cargo de la entrega.</li>
     * </ul>
     * El repartidor ya asignado se conserva: se cambia con el botón Asignar del
     * listado, no desde este formulario.
     *
     * @param id        identificador del pedido que se edita
     * @param direccion dirección de entrega nueva
     * @param tipo      tipo de pedido nuevo
     * @param estado    estado nuevo
     * @return {@code true} si el pedido fue actualizado; {@code false} si la fila
     *         ya no existe en la base de datos
     * @throws IllegalArgumentException si el pedido no existe o algún dato no es válido
     * @throws IllegalStateException    si el estado pedido no es posible para ese pedido
     */
    public boolean editarPedido(int id, String direccion, TipoPedido tipo, EstadoPedido estado) {
        Pedido actual = zonaDeCarga.buscarPedido(id);
        if (actual == null) {
            throw new IllegalArgumentException("No existe un pedido con el ID " + id + ".");
        }
        if (direccion == null || direccion.isBlank()) {
            throw new IllegalArgumentException("Debe indicar la dirección de entrega.");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Debe seleccionar el tipo de pedido.");
        }
        if (estado == null) {
            throw new IllegalArgumentException("Debe seleccionar el estado del pedido.");
        }

        String repartidorAsignado = actual.getRepartidorAsignado();
        if (estado != EstadoPedido.PENDIENTE && repartidorAsignado == null) {
            throw new IllegalStateException("El pedido #" + id + " no puede quedar en estado "
                    + estado + " sin un repartidor asignado. "
                    + "Asigne primero un repartidor con el botón Asignar de la sección Pedidos.");
        }

        Pedido cambios = new Pedido(id, direccion.trim(), tipo, estado);
        cambios.setRepartidorAsignado(repartidorAsignado);

        if (!pedidoDAO.editar(cambios)) {
            return false;
        }

        cargarPedidosDesdeDB();
        return true;
    }

    /**
     * Busca un pedido directamente en la base de datos, sin pasar por la zona de
     * carga. Lo usa {@code EntregaController} para comprobar que el pedido de una
     * entrega existe realmente antes de guardarla.
     *
     * @param id identificador del pedido
     * @return el pedido guardado en MySQL, o {@code null} si no existe
     */
    public Pedido buscarPedidoEnBaseDeDatos(int id) {
        return pedidoDAO.buscarPorId(id);
    }

    /**
     * Busca un pedido de la zona de carga por su identificador.
     *
     * @param id identificador del pedido
     * @return el pedido con ese identificador, o {@code null} si no está en memoria
     */
    public Pedido buscarPedido(int id) {
        return zonaDeCarga.buscarPedido(id);
    }

    /**
     * Elimina un pedido de la base de datos y, si lo logra, también de la zona de
     * carga, para que la lista en memoria refleje lo mismo que MySQL.
     * <p>
     * No se elimina un pedido que tenga entregas registradas: la tabla
     * {@code entrega} lo referencia y MySQL rechazaría el borrado. La comprobación
     * se hace antes para poder explicar el motivo con claridad, y no se desactiva
     * ninguna llave foránea.
     *
     * @param id identificador del pedido que se quiere eliminar
     * @return {@code true} si el pedido fue eliminado
     * @throws IllegalStateException si el pedido tiene entregas registradas
     */
    public boolean eliminarPedido(int id) {
        int entregas = entregaDAO.contarPorPedido(id);
        if (entregas > 0) {
            throw new IllegalStateException("No se puede eliminar el pedido #" + id
                    + " porque tiene " + entregas
                    + (entregas == 1 ? " entrega registrada." : " entregas registradas.")
                    + " Elimine primero esas entregas en la sección Entregas.");
        }

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
     * @throws cl.duocuc.exception.PersistenciaException si la base de datos no
     *         responde. La zona de carga se limpia solo después de una lectura
     *         correcta, por lo que los pedidos en memoria no se pierden.
     */
    public int cargarPedidosDesdeDB() {
        List<Pedido> leidos = pedidoDAO.cargarDesdeDB();

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
        return getPedidosFiltrados(null, EstadoPedido.PENDIENTE);
    }

    /**
     * Obtiene los pedidos que cumplen con los filtros elegidos en la vista. Los
     * filtros solo seleccionan qué pedidos se muestran: no modifican ningún
     * registro ni consultan la base de datos, porque trabajan sobre los pedidos
     * que ya están cargados en la zona de carga.
     *
     * @param tipo   tipo por el que se filtra, o {@code null} para no filtrar por tipo
     * @param estado estado por el que se filtra, o {@code null} para no filtrar por estado
     * @return la lista de pedidos que cumplen los dos filtros
     */
    public List<Pedido> getPedidosFiltrados(TipoPedido tipo, EstadoPedido estado) {
        List<Pedido> filtrados = new ArrayList<>();
        for (Pedido pedido : zonaDeCarga.getPedidos()) {
            if (tipo != null && pedido.getTipo() != tipo) continue;
            if (estado != null && pedido.getEstado() != estado) continue;
            filtrados.add(pedido);
        }
        return filtrados;
    }

    /**
     * Obtiene los tipos de pedido que puede ofrecer la vista.
     *
     * @return los valores del enum {@link TipoPedido}
     */
    public TipoPedido[] getTiposDePedido() { return TipoPedido.values(); }

    /**
     * Obtiene los estados de pedido que puede ofrecer la vista, para los
     * formularios de edición y para el filtro del listado.
     *
     * @return los valores del enum {@link EstadoPedido}
     */
    public EstadoPedido[] getEstadosDePedido() { return EstadoPedido.values(); }

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
