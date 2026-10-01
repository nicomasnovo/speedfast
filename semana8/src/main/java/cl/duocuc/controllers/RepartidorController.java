package cl.duocuc.controllers;

import cl.duocuc.dao.EntregaDAO;
import cl.duocuc.dao.RepartidorDAO;
import cl.duocuc.model.Pedido;
import cl.duocuc.model.Repartidor;
import cl.duocuc.model.ZonaDeCarga;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Controlador de repartidores: asigna pedidos y coordina el reparto concurrente
 * ejecutando cada {@link Repartidor} en su propio hilo dentro de un
 * {@link ExecutorService}, tal como lo hacía la versión de consola.
 * <p>
 * Los repartidores se leen y se guardan en MySQL, pero este controlador ya no
 * habla directamente con la base de datos: delega toda la persistencia en
 * {@link RepartidorDAO}. La lista en memoria se mantiene porque es la que usan
 * las vistas y los hilos de reparto (DAO → controlador → lista → Swing).
 * <p>
 * El método {@link #iniciarEntregas()} es bloqueante porque espera a que los
 * repartidores terminen, por lo que la vista debe llamarlo fuera del Event
 * Dispatch Thread (por ejemplo, con un {@code SwingWorker}).
 */
public class RepartidorController {

    /** Tiempo máximo de espera para que el pool termine las entregas. */
    private static final int ESPERA_MAXIMA_SEGUNDOS = 60;

    private final ZonaDeCarga zonaDeCarga;
    private final RepartidorDAO repartidorDAO = new RepartidorDAO();

    /**
     * Solo se consulta para saber cuántas entregas dependen de un repartidor antes
     * de eliminarlo. Las entregas las gestiona {@link EntregaController}.
     */
    private final EntregaDAO entregaDAO = new EntregaDAO();

    private final List<Repartidor> repartidores = new ArrayList<>();

    /** Indica si hay un reparto en ejecución; se consulta desde la interfaz. */
    private volatile boolean entregasEnCurso;

    /**
     * Crea el controlador con un repartidor por cada nombre recibido. Si no se
     * entregan nombres, la lista parte vacía y se debe llenar con
     * {@link #cargarRepartidoresDesdeDB()}.
     *
     * @param zonaDeCarga zona de carga compartida desde la que retiran los pedidos
     * @param nombres     nombres de los repartidores disponibles
     */
    public RepartidorController(ZonaDeCarga zonaDeCarga, String... nombres) {
        this.zonaDeCarga = zonaDeCarga;
        for (String nombre : nombres) {
            repartidores.add(new Repartidor(nombre, zonaDeCarga));
        }
    }

    /**
     * Lee los repartidores guardados en MySQL y rearma con ellos la lista en
     * memoria.
     * <p>
     * La lista se limpia antes de cargar, por lo que el método se puede llamar
     * todas las veces que se quiera sin duplicar repartidores.
     *
     * @return la cantidad de repartidores cargados desde la base de datos
     * @throws cl.duocuc.exception.PersistenciaException si la base de datos no
     *         responde. La lista en memoria se limpia solo después de una lectura
     *         correcta, por lo que los repartidores cargados no se pierden.
     */
    public int cargarRepartidoresDesdeDB() {
        List<Repartidor> leidos = repartidorDAO.cargarDesdeDB(zonaDeCarga);

        repartidores.clear();
        repartidores.addAll(leidos);
        return repartidores.size();
    }

    /**
     * Guarda un repartidor nuevo en la base de datos y lo agrega a la lista en
     * memoria con el identificador que generó MySQL.
     *
     * @param nombre nombre del repartidor
     * @return el repartidor creado, o {@code null} si no se pudo guardar
     * @throws IllegalArgumentException si el nombre está vacío o ya existe
     */
    public Repartidor agregarRepartidor(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("Debe indicar el nombre del repartidor.");
        }
        String nombreLimpio = nombre.trim();
        if (getNombresRepartidores().contains(nombreLimpio)) {
            throw new IllegalArgumentException("Ya existe un repartidor llamado " + nombreLimpio + ".");
        }

        Repartidor repartidor = new Repartidor(nombreLimpio, zonaDeCarga);
        if (!repartidorDAO.agregar(repartidor)) {
            return null;
        }

        repartidores.add(repartidor);
        return repartidor;
    }

    /**
     * Cambia el nombre de un repartidor en la base de datos y, si lo logra,
     * también en la lista en memoria.
     *
     * @param id     identificador del repartidor
     * @param nombre nuevo nombre del repartidor
     * @return {@code true} si el repartidor fue actualizado; {@code false} si la
     *         fila ya no existe en la base de datos
     * @throws IllegalArgumentException si el nombre está vacío o ya lo usa otro repartidor
     */
    public boolean editarRepartidor(int id, String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("Debe indicar el nombre del repartidor.");
        }
        String nombreLimpio = nombre.trim();

        for (Repartidor otro : repartidores) {
            if (otro.getId() != id && otro.getNombre().equals(nombreLimpio)) {
                throw new IllegalArgumentException("Ya existe otro repartidor llamado " + nombreLimpio + ".");
            }
        }

        if (!repartidorDAO.editar(id, nombreLimpio)) {
            return false;
        }

        Repartidor repartidor = buscarRepartidor(id);
        if (repartidor != null) {
            repartidor.setNombre(nombreLimpio);
        }
        return true;
    }

    /**
     * Elimina un repartidor de la base de datos y, si lo logra, también de la
     * lista en memoria.
     *
     * No se elimina un repartidor que tenga entregas registradas: la tabla
     * {@code entrega} lo referencia y MySQL rechazaría el borrado. La comprobación
     * se hace antes para poder explicar el motivo con claridad, y no se desactiva
     * ninguna llave foránea.
     *
     * @param id identificador del repartidor
     * @return {@code true} si el repartidor fue eliminado; {@code false} si la
     *         fila ya no existe en la base de datos
     * @throws IllegalStateException si hay un reparto en curso, porque el
     *                               repartidor podría estar entregando un pedido,
     *                               o si el repartidor tiene entregas registradas
     */
    public boolean eliminarRepartidor(int id) {
        if (entregasEnCurso) {
            throw new IllegalStateException("No se puede eliminar un repartidor mientras hay un reparto en curso.");
        }

        int entregas = entregaDAO.contarPorRepartidor(id);
        if (entregas > 0) {
            Repartidor repartidor = buscarRepartidor(id);
            String quien = repartidor != null
                    ? "al repartidor " + repartidor.getNombre()
                    : "el repartidor con ID " + id;
            throw new IllegalStateException("No se puede eliminar " + quien
                    + " porque tiene " + entregas
                    + (entregas == 1 ? " entrega registrada." : " entregas registradas.")
                    + " Elimine primero esas entregas en la sección Entregas.");
        }

        if (!repartidorDAO.eliminar(id)) {
            return false;
        }

        repartidores.removeIf(repartidor -> repartidor.getId() == id);
        return true;
    }

    /**
     * Busca un repartidor de la lista en memoria por su identificador.
     *
     * @param id identificador del repartidor
     * @return el repartidor con ese identificador, o {@code null} si no existe
     */
    public Repartidor buscarRepartidor(int id) {
        for (Repartidor repartidor : repartidores) {
            if (repartidor.getId() == id) return repartidor;
        }
        return null;
    }

    /**
     * Busca un repartidor directamente en la base de datos, sin pasar por la lista
     * en memoria. Lo usa {@code EntregaController} para comprobar que el repartidor
     * de una entrega existe realmente antes de guardarla.
     *
     * @param id identificador del repartidor
     * @return el repartidor guardado en MySQL, o {@code null} si no existe
     */
    public Repartidor buscarRepartidorEnBaseDeDatos(int id) {
        return repartidorDAO.buscarPorId(id, zonaDeCarga);
    }

    /**
     * Obtiene los repartidores del sistema.
     *
     * @return la lista de repartidores
     */
    public List<Repartidor> getRepartidores() { return List.copyOf(repartidores); }

    /**
     * Obtiene los nombres de los repartidores, para mostrarlos en la vista.
     *
     * @return la lista de nombres
     */
    public List<String> getNombresRepartidores() {
        List<String> nombres = new ArrayList<>();
        for (Repartidor repartidor : repartidores) {
            nombres.add(repartidor.getNombre());
        }
        return nombres;
    }

    /**
     * Asigna un pedido pendiente a un repartidor, reservándolo en la zona de carga.
     *
     * @param idPedido         identificador del pedido
     * @param nombreRepartidor nombre del repartidor que se hará cargo
     * @return el pedido asignado
     * @throws IllegalArgumentException si el pedido o el repartidor no existen
     * @throws IllegalStateException    si el pedido ya no está disponible
     */
    public Pedido asignarRepartidor(int idPedido, String nombreRepartidor) {
        Pedido pedido = zonaDeCarga.buscarPedido(idPedido);
        if (pedido == null) {
            throw new IllegalArgumentException("No existe un pedido con el ID " + idPedido + ".");
        }
        if (nombreRepartidor == null || !getNombresRepartidores().contains(nombreRepartidor)) {
            throw new IllegalArgumentException("Debe seleccionar un repartidor válido.");
        }
        zonaDeCarga.asignarRepartidor(pedido, nombreRepartidor);
        return pedido;
    }

    /**
     * Indica si en este momento se está ejecutando un reparto.
     *
     * @return {@code true} si hay entregas en curso
     */
    public boolean isEntregasEnCurso() { return entregasEnCurso; }

    /**
     * Inicia el reparto concurrente: lanza un hilo por repartidor y espera a que
     * terminen de vaciar la zona de carga.
     *
     * @return el resumen del despacho una vez terminadas las entregas
     * @throws InterruptedException  si el hilo que llama es interrumpido mientras espera
     * @throws IllegalStateException si ya hay un reparto en curso o no hay repartidores
     */
    public String iniciarEntregas() throws InterruptedException {
        synchronized (this) {
            if (entregasEnCurso) {
                throw new IllegalStateException("Ya hay un reparto en curso.");
            }
            if (repartidores.isEmpty()) {
                throw new IllegalStateException("No hay repartidores registrados en la base de datos.");
            }
            entregasEnCurso = true;
        }
        try {
            ExecutorService executor = Executors.newFixedThreadPool(repartidores.size());
            for (Repartidor repartidor : repartidores) {
                executor.submit(repartidor);
            }
            executor.shutdown();
            if (!executor.awaitTermination(ESPERA_MAXIMA_SEGUNDOS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            return getResumen();
        } finally {
            entregasEnCurso = false;
        }
    }

    /**
     * Arma el resumen del despacho con las entregas de cada repartidor y el estado
     * general de la zona de carga.
     *
     * @return el resumen del despacho en texto
     */
    public String getResumen() {
        StringBuilder resumen = new StringBuilder();
        for (Repartidor repartidor : repartidores) {
            resumen.append("- ").append(repartidor.getNombre())
                    .append(" entregó ").append(repartidor.getEntregas())
                    .append(" pedido(s).\n");
        }
        resumen.append("\nEntregas registradas: ").append(zonaDeCarga.getEntregados())
                .append(" de ").append(zonaDeCarga.getTotalPedidos()).append(" pedidos.\n\n");
        resumen.append(zonaDeCarga.todosEntregados()
                ? "Todos los pedidos han sido entregados correctamente."
                : "Quedaron pedidos sin entregar en la zona de carga.");
        return resumen.toString();
    }
}
