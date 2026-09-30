package cl.duocuc.controllers;

import cl.duocuc.dao.EntregaDAO;
import cl.duocuc.model.Entrega;
import cl.duocuc.model.EstadoPedido;
import cl.duocuc.model.Pedido;
import cl.duocuc.model.Repartidor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Controlador de entregas: valida los datos que llegan de la vista, aplica las
 * reglas de negocio y delega la persistencia de la tabla {@code entrega} en
 * {@link EntregaDAO}. No contiene SQL ni componentes Swing.
 * <p>
 * Para no duplicar reglas que ya existían, este controlador reutiliza los
 * controladores de pedidos y repartidores en vez de repetir su lógica:
 * <ul>
 *   <li>{@link PedidoController} le dice si el pedido existe y guarda sus
 *       cambios;</li>
 *   <li>{@link RepartidorController} le dice si el repartidor existe y es el que
 *       sabe reservar un pedido para un repartidor con
 *       {@link RepartidorController#asignarRepartidor(int, String)}.</li>
 * </ul>
 * Las reglas propias de las entregas son:
 * <ul>
 *   <li>pedido, repartidor, fecha y hora son obligatorios;</li>
 *   <li>el pedido y el repartidor deben existir en la base de datos, para no
 *       chocar con las llaves foráneas de la tabla {@code entrega};</li>
 *   <li>un pedido puede tener una sola entrega registrada;</li>
 *   <li>registrar la entrega de un pedido que todavía está
 *       {@link EstadoPedido#PENDIENTE} y sin repartidor lo deja asignado al
 *       repartidor de esa entrega. El cambio de estado a
 *       {@link EstadoPedido#EN_REPARTO} y {@link EstadoPedido#ENTREGADO} lo sigue
 *       haciendo el reparto de la sección Registrar Entrega, que no se toca.</li>
 * </ul>
 */
public class EntregaController {

    /** Formato en que la vista escribe y muestra la fecha. */
    public static final String FORMATO_FECHA = "yyyy-MM-dd";

    /** Formato en que la vista escribe y muestra la hora. */
    public static final String FORMATO_HORA = "HH:mm";

    private final EntregaDAO entregaDAO = new EntregaDAO();
    private final PedidoController pedidoController;
    private final RepartidorController repartidorController;

    /**
     * Crea el controlador de entregas sobre los controladores que ya coordinan los
     * pedidos y los repartidores.
     *
     * @param pedidoController     controlador de pedidos compartido
     * @param repartidorController controlador de repartidores compartido
     */
    public EntregaController(PedidoController pedidoController,
                             RepartidorController repartidorController) {
        this.pedidoController = pedidoController;
        this.repartidorController = repartidorController;
    }

    /**
     * Obtiene todas las entregas registradas, con la dirección del pedido y el
     * nombre del repartidor ya resueltos para mostrarlos en la tabla.
     *
     * @return la lista de entregas guardadas en MySQL
     * @throws cl.duocuc.exception.PersistenciaException si la base de datos no responde
     */
    public List<Entrega> listarEntregas() {
        return entregaDAO.listarTodos();
    }

    /**
     * Busca una entrega por su identificador.
     *
     * @param id identificador de la entrega
     * @return la entrega encontrada, o {@code null} si no existe
     */
    public Entrega buscarEntrega(int id) {
        return entregaDAO.buscarPorId(id);
    }

    /**
     * Registra una entrega con la fecha y la hora tal como las escribió el usuario.
     *
     * @param idPedido     identificador del pedido entregado
     * @param idRepartidor identificador del repartidor que la realizó
     * @param fechaTexto   fecha en formato {@value #FORMATO_FECHA}
     * @param horaTexto    hora en formato {@value #FORMATO_HORA}
     * @return la entrega registrada, o {@code null} si no se pudo guardar
     * @throws IllegalArgumentException si algún dato falta o no es válido
     */
    public Entrega registrarEntrega(int idPedido, int idRepartidor,
                                    String fechaTexto, String horaTexto) {
        return registrarEntrega(idPedido, idRepartidor,
                convertirFecha(fechaTexto), convertirHora(horaTexto));
    }

    /**
     * Registra una entrega con los datos ya convertidos: valida, la guarda en
     * MySQL y, si el pedido seguía pendiente y sin repartidor, se lo asigna al
     * repartidor de la entrega reutilizando {@link RepartidorController}.
     *
     * @param idPedido     identificador del pedido entregado
     * @param idRepartidor identificador del repartidor que la realizó
     * @param fecha        fecha de la entrega
     * @param hora         hora de la entrega
     * @return la entrega registrada, o {@code null} si no se pudo guardar
     * @throws IllegalArgumentException si algún dato falta o no es válido
     */
    public Entrega registrarEntrega(int idPedido, int idRepartidor,
                                    LocalDate fecha, LocalTime hora) {
        validarDatos(idPedido, idRepartidor, fecha, hora);

        Entrega existente = entregaDAO.buscarPorPedido(idPedido);
        if (existente != null) {
            throw new IllegalArgumentException("El pedido #" + idPedido
                    + " ya tiene registrada la entrega #" + existente.getId() + ".");
        }

        Entrega entrega = new Entrega(idPedido, idRepartidor, fecha, hora);
        if (!entregaDAO.agregar(entrega)) {
            return null;
        }

        asignarRepartidorSiCorresponde(idPedido, idRepartidor);
        return entrega;
    }

    /**
     * Edita una entrega con la fecha y la hora tal como las escribió el usuario.
     *
     * @param id           identificador de la entrega que se edita
     * @param idPedido     identificador del pedido entregado
     * @param idRepartidor identificador del repartidor que la realizó
     * @param fechaTexto   fecha en formato {@value #FORMATO_FECHA}
     * @param horaTexto    hora en formato {@value #FORMATO_HORA}
     * @return {@code true} si la entrega fue actualizada
     * @throws IllegalArgumentException si algún dato falta o no es válido
     */
    public boolean editarEntrega(int id, int idPedido, int idRepartidor,
                                 String fechaTexto, String horaTexto) {
        return editarEntrega(id, idPedido, idRepartidor,
                convertirFecha(fechaTexto), convertirHora(horaTexto));
    }

    /**
     * Edita una entrega con los datos ya convertidos: valida y guarda los cambios
     * en MySQL.
     *
     * @param id           identificador de la entrega que se edita
     * @param idPedido     identificador del pedido entregado
     * @param idRepartidor identificador del repartidor que la realizó
     * @param fecha        fecha de la entrega
     * @param hora         hora de la entrega
     * @return {@code true} si la entrega fue actualizada; {@code false} si la fila
     *         ya no existe en la base de datos
     * @throws IllegalArgumentException si algún dato falta o no es válido
     */
    public boolean editarEntrega(int id, int idPedido, int idRepartidor,
                                 LocalDate fecha, LocalTime hora) {
        validarDatos(idPedido, idRepartidor, fecha, hora);

        // El pedido puede cambiar al editar, así que se vuelve a revisar que el
        // pedido elegido no tenga ya otra entrega distinta de esta.
        Entrega delPedido = entregaDAO.buscarPorPedido(idPedido);
        if (delPedido != null && delPedido.getId() != id) {
            throw new IllegalArgumentException("El pedido #" + idPedido
                    + " ya tiene registrada la entrega #" + delPedido.getId() + ".");
        }

        Entrega entrega = new Entrega(id, idPedido, idRepartidor, fecha, hora);
        if (!entregaDAO.editar(entrega)) {
            return false;
        }

        asignarRepartidorSiCorresponde(idPedido, idRepartidor);
        return true;
    }

    /**
     * Elimina una entrega de la base de datos. El pedido y el repartidor no se
     * tocan: borrar el registro de la entrega no borra ni el pedido ni la persona
     * que lo repartió.
     *
     * @param id identificador de la entrega
     * @return {@code true} si la entrega fue eliminada; {@code false} si la fila ya
     *         no existe en la base de datos
     */
    public boolean eliminarEntrega(int id) {
        return entregaDAO.eliminar(id);
    }

    /**
     * Obtiene los pedidos que la vista puede ofrecer para registrar una entrega.
     * Son todos los pedidos registrados, porque una entrega también se puede
     * registrar después de que el pedido fue entregado.
     *
     * @return la lista de pedidos del sistema
     */
    public List<Pedido> getPedidosDisponibles() {
        return pedidoController.getPedidos();
    }

    /**
     * Obtiene los repartidores que la vista puede ofrecer para registrar una
     * entrega.
     *
     * @return la lista de repartidores del sistema
     */
    public List<Repartidor> getRepartidoresDisponibles() {
        return repartidorController.getRepartidores();
    }

    /**
     * Comprueba que los datos de la entrega estén completos y que el pedido y el
     * repartidor existan en la base de datos. Revisar la existencia antes de
     * guardar evita que MySQL rechace la operación por las llaves foráneas.
     *
     * @param idPedido     identificador del pedido
     * @param idRepartidor identificador del repartidor
     * @param fecha        fecha de la entrega
     * @param hora         hora de la entrega
     * @throws IllegalArgumentException si algún dato falta o no existe
     */
    private void validarDatos(int idPedido, int idRepartidor, LocalDate fecha, LocalTime hora) {
        if (idPedido <= 0) {
            throw new IllegalArgumentException("Debe seleccionar el pedido de la entrega.");
        }
        if (idRepartidor <= 0) {
            throw new IllegalArgumentException("Debe seleccionar el repartidor de la entrega.");
        }
        if (fecha == null) {
            throw new IllegalArgumentException("Debe indicar la fecha de la entrega.");
        }
        if (hora == null) {
            throw new IllegalArgumentException("Debe indicar la hora de la entrega.");
        }
        if (pedidoController.buscarPedidoEnBaseDeDatos(idPedido) == null) {
            throw new IllegalArgumentException(
                    "No existe el pedido #" + idPedido + " en la base de datos.");
        }
        if (repartidorController.buscarRepartidorEnBaseDeDatos(idRepartidor) == null) {
            throw new IllegalArgumentException("No existe el repartidor con ID "
                    + idRepartidor + " en la base de datos.");
        }
    }

    /**
     * Deja el pedido a cargo del repartidor de la entrega cuando el pedido todavía
     * está pendiente y no tiene a nadie asignado. Reutiliza la asignación que ya
     * existía en {@link RepartidorController} y la persiste con
     * {@link PedidoController#editarPedido(Pedido)}.
     * <p>
     * Si el pedido ya está en reparto, entregado o ya tenía un repartidor, no se
     * cambia nada: la entrega solo queda registrada.
     *
     * @param idPedido     identificador del pedido de la entrega
     * @param idRepartidor identificador del repartidor de la entrega
     */
    private void asignarRepartidorSiCorresponde(int idPedido, int idRepartidor) {
        Pedido pedido = pedidoController.buscarPedido(idPedido);
        if (pedido == null
                || pedido.getEstado() != EstadoPedido.PENDIENTE
                || pedido.getRepartidorAsignado() != null) {
            return;
        }

        Repartidor repartidor = repartidorController.buscarRepartidor(idRepartidor);
        if (repartidor == null) {
            return;
        }

        try {
            repartidorController.asignarRepartidor(idPedido, repartidor.getNombre());
            pedidoController.editarPedido(pedido);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // El pedido ya fue tomado por un repartidor mientras se registraba la
            // entrega. La entrega queda guardada igual y el aviso va a la consola,
            // porque no es un error que deba detener el registro.
            System.out.println("[EntregaController] El pedido #" + idPedido
                    + " no se pudo asignar a " + repartidor.getNombre()
                    + ": " + e.getMessage());
        }
    }

    /**
     * Convierte la fecha escrita en la vista a {@link LocalDate}.
     *
     * @param fechaTexto fecha en formato {@value #FORMATO_FECHA}
     * @return la fecha convertida
     * @throws IllegalArgumentException si el texto está vacío o no tiene el formato esperado
     */
    private LocalDate convertirFecha(String fechaTexto) {
        if (fechaTexto == null || fechaTexto.isBlank()) {
            throw new IllegalArgumentException("Debe indicar la fecha de la entrega.");
        }
        try {
            return LocalDate.parse(fechaTexto.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "La fecha debe tener el formato " + FORMATO_FECHA + " (por ejemplo 2026-09-30).");
        }
    }

    /**
     * Convierte la hora escrita en la vista a {@link LocalTime}.
     *
     * @param horaTexto hora en formato {@value #FORMATO_HORA}
     * @return la hora convertida
     * @throws IllegalArgumentException si el texto está vacío o no tiene el formato esperado
     */
    private LocalTime convertirHora(String horaTexto) {
        if (horaTexto == null || horaTexto.isBlank()) {
            throw new IllegalArgumentException("Debe indicar la hora de la entrega.");
        }
        try {
            return LocalTime.parse(horaTexto.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "La hora debe tener el formato " + FORMATO_HORA + " (por ejemplo 14:30).");
        }
    }
}
