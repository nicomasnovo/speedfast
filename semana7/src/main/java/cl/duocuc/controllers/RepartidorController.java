package cl.duocuc.controllers;

import cl.duocuc.model.Pedido;
import cl.duocuc.model.Repartidor;
import cl.duocuc.model.ZonaDeCarga;

import javax.swing.JOptionPane;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
 * Desde la semana 7 los repartidores se leen y se guardan en MySQL a través de
 * {@link ConexionBD#obtenerConexion()}; la lista en memoria se mantiene porque es
 * la que usan las vistas y los hilos de reparto (MySQL → controlador → lista →
 * Swing).
 * <p>
 * El método {@link #iniciarEntregas()} es bloqueante porque espera a que los
 * repartidores terminen, por lo que la vista debe llamarlo fuera del Event
 * Dispatch Thread (por ejemplo, con un {@code SwingWorker}).
 */
public class RepartidorController {

    //Se crean las variables SQL para mejorar la legibilidad de las clases creadas
    /** Tiempo máximo de espera para que el pool termine las entregas. */
    private static final int ESPERA_MAXIMA_SEGUNDOS = 60;

    /** Consulta que trae todos los repartidores guardados. */
    private static final String SQL_SELECT = "SELECT id, nombre FROM repartidor ORDER BY id";

    /** Inserción de un repartidor nuevo; el identificador lo genera MySQL. */
    private static final String SQL_INSERT = "INSERT INTO repartidor (nombre) VALUES (?)";

    /** Actualización del nombre de un repartidor existente. */
    private static final String SQL_UPDATE = "UPDATE repartidor SET nombre = ? WHERE id = ?";

    /** Eliminación de un repartidor por su identificador. */
    private static final String SQL_DELETE = "DELETE FROM repartidor WHERE id = ?";

    private final ZonaDeCarga zonaDeCarga;
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
     */
    public int cargarRepartidoresDesdeDB() {
        List<Repartidor> leidos = new ArrayList<>();

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                leidos.add(new Repartidor(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        zonaDeCarga));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "Error al cargar los repartidores desde la base de datos.");
            return 0;
        }

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
            throw new IllegalArgumentException(
                    "Ya existe un repartidor llamado " + nombreLimpio + ".");
        }

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement stmt =
                     conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, nombreLimpio);
            if (stmt.executeUpdate() != 1) {
                return null;
            }

            int id = 0;
            try (ResultSet claves = stmt.getGeneratedKeys()) {
                if (claves.next()) {
                    id = claves.getInt(1);
                }
            }

            Repartidor repartidor = new Repartidor(id, nombreLimpio, zonaDeCarga);
            repartidores.add(repartidor);
            return repartidor;

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "Error al guardar el repartidor en la base de datos.");
            return null;
        }
    }

    /**
     * Cambia el nombre de un repartidor en la base de datos y, si lo logra,
     * también en la lista en memoria.
     *
     * @param id     identificador del repartidor
     * @param nombre nuevo nombre del repartidor
     * @return {@code true} si el repartidor fue actualizado
     * @throws IllegalArgumentException si el nombre está vacío
     */
    public boolean editarRepartidor(int id, String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("Debe indicar el nombre del repartidor.");
        }
        String nombreLimpio = nombre.trim();

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {

            stmt.setString(1, nombreLimpio);
            stmt.setInt(2, id);

            int filas = stmt.executeUpdate();
            if (filas == 0) {
                JOptionPane.showMessageDialog(
                        null,
                        "No existe el repartidor con ID " + id + " en la base de datos.");
                return false;
            }

            Repartidor repartidor = buscarRepartidor(id);
            if (repartidor != null) {
                repartidor.setNombre(nombreLimpio);
            }
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "Error al actualizar el repartidor en la base de datos.");
            return false;
        }
    }

    /**
     * Elimina un repartidor de la base de datos y, si lo logra, también de la
     * lista en memoria.
     *
     * @param id identificador del repartidor
     * @return {@code true} si el repartidor fue eliminado
     */
    public boolean eliminarRepartidor(int id) {
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {

            stmt.setInt(1, id);

            int filas = stmt.executeUpdate();
            if (filas == 0) {
                JOptionPane.showMessageDialog(
                        null,
                        "No existe el repartidor con ID " + id + " en la base de datos.");
                return false;
            }

            repartidores.removeIf(repartidor -> repartidor.getId() == id);
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "No se pudo eliminar el repartidor con ID " + id
                            + ": tiene entregas asociadas o hubo un error en la base de datos.");
            return false;
        }
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
                throw new IllegalStateException(
                        "No hay repartidores registrados en la base de datos.");
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
