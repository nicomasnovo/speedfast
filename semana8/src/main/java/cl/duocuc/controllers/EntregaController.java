package cl.duocuc.controllers;

import cl.duocuc.dao.EntregaDAO;
import cl.duocuc.model.Entrega;
import cl.duocuc.model.EstadoPedido;
import cl.duocuc.model.Pedido;
import cl.duocuc.model.Repartidor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de entregas: valida los datos que llegan de la vista, aplica las
 * reglas de negocio y delega la persistencia de la tabla {@code entrega} en
 * {@link EntregaDAO}. No contiene SQL ni componentes Swing.
 * <p>
 * Para no duplicar reglas que ya existían, este controlador reutiliza los
 * controladores de pedidos y repartidores en vez de repetir su lógica:
 * <ul>
 *   <li>{@link PedidoController} le dice si el pedido existe, lo cierra como
 *       entregado y guarda sus cambios;</li>
 *   <li>{@link RepartidorController} le dice si el repartidor existe.</li>
 * </ul>
 * Las reglas propias de las entregas son:
 * <ul>
 *   <li>pedido, repartidor, fecha y hora son obligatorios;</li>
 *   <li>el pedido y el repartidor deben existir en la base de datos, para no
 *       chocar con las llaves foráneas de la tabla {@code entrega};</li>
 *   <li>un pedido puede tener una sola entrega registrada;</li>
 *   <li>registrar una entrega significa que el pedido llegó a su destino: queda en
 *       estado {@link EstadoPedido#ENTREGADO}, a nombre del repartidor de la
 *       entrega y contado como entregado. Registrar una entrega no es lo mismo que
 *       asignar un pedido: la asignación, que solo reserva un pedido
 *       {@link EstadoPedido#PENDIENTE} para un repartidor, se hace con el botón
 *       Asignar de la sección Pedidos.</li>
 *   <li>lo mismo vale al revés: los pedidos que los repartidores entregan durante
 *       el reparto quedan anotados en la tabla {@code entrega} con
 *       {@link #registrarEntregasDelReparto()}, para que la sección Entregas sea
 *       siempre la lista de los pedidos entregados.</li>
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
    public EntregaController(PedidoController pedidoController, RepartidorController repartidorController) {
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
    public Entrega registrarEntrega(int idPedido, int idRepartidor, String fechaTexto, String horaTexto) {
        return registrarEntrega(idPedido, idRepartidor, convertirFecha(fechaTexto), convertirHora(horaTexto));
    }

    /**
     * Registra una entrega con los datos ya convertidos: valida, la guarda en
     * MySQL y deja el pedido entregado por el repartidor de la entrega.
     *
     * @param idPedido     identificador del pedido entregado
     * @param idRepartidor identificador del repartidor que la realizó
     * @param fecha        fecha de la entrega
     * @param hora         hora de la entrega
     * @return la entrega registrada, o {@code null} si no se pudo guardar
     * @throws IllegalArgumentException si algún dato falta o no es válido
     */
    public Entrega registrarEntrega(int idPedido, int idRepartidor, LocalDate fecha, LocalTime hora) {
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

        cerrarPedidoComoEntregado(idPedido, idRepartidor);
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
    public boolean editarEntrega(int id, int idPedido, int idRepartidor, String fechaTexto, String horaTexto) {
        return editarEntrega(id, idPedido, idRepartidor,
                convertirFecha(fechaTexto), convertirHora(horaTexto));
    }

    /**
     * Edita una entrega con los datos ya convertidos: valida, guarda los cambios en
     * MySQL y deja el pedido entregado por el repartidor que quedó en la entrega.
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
    public boolean editarEntrega(int id, int idPedido, int idRepartidor, LocalDate fecha, LocalTime hora) {
        validarDatos(idPedido, idRepartidor, fecha, hora);

        Entrega delPedido = entregaDAO.buscarPorPedido(idPedido);
        if (delPedido != null && delPedido.getId() != id) {
            throw new IllegalArgumentException("El pedido #" + idPedido
                    + " ya tiene registrada la entrega #" + delPedido.getId() + ".");
        }

        Entrega entrega = new Entrega(id, idPedido, idRepartidor, fecha, hora);
        if (!entregaDAO.editar(entrega)) {
            return false;
        }

        cerrarPedidoComoEntregado(idPedido, idRepartidor);
        return true;
    }

    /**
     * Anota en la tabla {@code entrega} los pedidos que ya figuran entregados y
     * todavía no tienen comprobante. La usa la sección Pedidos después de un
     * reparto: los hilos de los repartidores dejan los pedidos en
     * {@link EstadoPedido#ENTREGADO}, y aquí cada uno de esos pedidos queda además
     * listado como entrega del repartidor que se hizo cargo, con la fecha y la hora
     * en que se anotó.
     * <p>
     * Un pedido que ya tenía su entrega registrada se salta, de modo que repetir el
     * reparto no duplique comprobantes. También se saltan los pedidos cuyo
     * repartidor no está en la lista de repartidores, porque la tabla
     * {@code entrega} necesita su identificador.
     *
     * @return la cantidad de entregas que quedaron anotadas
     * @throws cl.duocuc.exception.PersistenciaException si la base de datos no responde
     */
    public int registrarEntregasDelReparto() {
        List<Integer> conComprobante = new ArrayList<>();
        for (Entrega registrada : entregaDAO.listarTodos()) {
            conComprobante.add(registrada.getIdPedido());
        }

        int anotadas = 0;
        for (Pedido pedido : pedidoController.getPedidos()) {
            if (pedido.getEstado() != EstadoPedido.ENTREGADO || conComprobante.contains(pedido.getId())) {
                continue;
            }

            Repartidor repartidor = buscarRepartidorPorNombre(pedido.getRepartidorAsignado());
            if (repartidor == null || repartidor.getId() <= 0) {
                System.out.println("[EntregaController] El pedido #" + pedido.getId()
                        + " está entregado, pero su repartidor ("
                        + pedido.getRepartidorAsignado()
                        + ") no permite anotar la entrega.");
                continue;
            }

            Entrega entrega = new Entrega(pedido.getId(), repartidor.getId(), LocalDate.now(), LocalTime.now().withSecond(0).withNano(0));
            if (entregaDAO.agregar(entrega)) {
                anotadas++;
            }
        }
        return anotadas;
    }

    /**
     * Busca entre los repartidores cargados al que lleva un nombre. El pedido
     * guarda el nombre del repartidor y la tabla {@code entrega} necesita su
     * identificador, así que hay que traducir uno al otro.
     *
     * @param nombre nombre del repartidor, tal como lo guarda el pedido
     * @return el repartidor con ese nombre, o {@code null} si no hay ninguno
     */
    private Repartidor buscarRepartidorPorNombre(String nombre) {
        if (nombre == null) {
            return null;
        }
        for (Repartidor repartidor : repartidorController.getRepartidores()) {
            if (nombre.equals(repartidor.getNombre())) {
                return repartidor;
            }
        }
        return null;
    }

    /**
     * Elimina una entrega de la base de datos. El pedido y el repartidor no se
     * tocan: borrar el registro de la entrega no borra ni el pedido ni la persona
     * que lo repartió, y el pedido sigue entregado, porque lo que se borra es el
     * comprobante y no el hecho de que llegó a su destino. Para devolver un pedido
     * a {@link EstadoPedido#PENDIENTE} se usa el botón Editar de la sección Pedidos.
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
     * Cierra el pedido de la entrega: lo deja en estado
     * {@link EstadoPedido#ENTREGADO} a nombre del repartidor que la realizó,
     * delegando en {@link PedidoController#marcarEntregado(int, String)}, y le suma
     * la entrega a ese repartidor.
     * <p>
     * Si el pedido ya figuraba como entregado solo cambia el repartidor y la
     * entrega no se vuelve a contar, de modo que corregir una entrega existente no
     * infle los totales.
     *
     * @param idPedido     identificador del pedido de la entrega
     * @param idRepartidor identificador del repartidor de la entrega
     */
    private void cerrarPedidoComoEntregado(int idPedido, int idRepartidor) {
        Pedido pedido = pedidoController.buscarPedido(idPedido);
        Repartidor repartidor = repartidorController.buscarRepartidor(idRepartidor);
        if (pedido == null || repartidor == null) {
            return;
        }

        boolean yaEstabaEntregado = pedido.getEstado() == EstadoPedido.ENTREGADO;
        try {
            pedidoController.marcarEntregado(idPedido, repartidor.getNombre());
            if (!yaEstabaEntregado) {
                repartidor.sumarEntrega();
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("[EntregaController] El pedido #" + idPedido
                    + " no se pudo cerrar como entregado por " + repartidor.getNombre()
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
