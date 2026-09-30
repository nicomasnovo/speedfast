package cl.duocuc.dao;

import cl.duocuc.conexion.ConexionBD;
import cl.duocuc.exception.PersistenciaException;
import cl.duocuc.model.Entrega;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de la tabla {@code entrega}. Es el único lugar donde vive el SQL
 * de las entregas y sigue la misma forma de trabajo que {@code PedidoDAO} y
 * {@code RepartidorDAO}: pide las conexiones a
 * {@link ConexionBD#obtenerConexion()}, usa siempre {@link PreparedStatement} y
 * {@link ResultSet}, y cierra todo con try-with-resources.
 * <p>
 * No contiene reglas de negocio ni mensajes de Swing: solo persiste. Las reglas
 * (qué pedido puede recibir una entrega, si el pedido ya tiene una, etc.) están
 * en {@code EntregaController}, y los avisos al usuario los muestra la vista.
 * <p>
 * Las consultas de lectura traen además la dirección del pedido y el nombre del
 * repartidor con un JOIN, de modo que la tabla de la interfaz pueda mostrar datos
 * entendibles en lugar de solo identificadores.
 */
public class EntregaDAO {

    //Se crean las variables SQL para mejorar la legibilidad de las clases creadas
    /**
     * Parte común de las consultas de lectura. Incluye los JOIN con {@code pedido}
     * y {@code repartidor} para obtener los datos que se muestran en la interfaz.
     */
    private static final String SQL_SELECT_BASE =
            "SELECT e.id, e.id_pedido, e.id_repartidor, e.fecha, e.hora, "
                    + "p.direccion AS direccion_pedido, r.nombre AS nombre_repartidor "
                    + "FROM entrega e "
                    + "JOIN pedido p ON p.id = e.id_pedido "
                    + "JOIN repartidor r ON r.id = e.id_repartidor ";

    /** Consulta que trae todas las entregas guardadas. */
    private static final String SQL_SELECT = SQL_SELECT_BASE + "ORDER BY e.id";

    /** Consulta que trae una entrega por su identificador. */
    private static final String SQL_SELECT_BY_ID = SQL_SELECT_BASE + "WHERE e.id = ?";

    /** Consulta que trae la entrega registrada para un pedido, si existe. */
    private static final String SQL_SELECT_BY_PEDIDO = SQL_SELECT_BASE + "WHERE e.id_pedido = ?";

    /** Cuenta las entregas que dependen de un pedido. */
    private static final String SQL_CONTAR_POR_PEDIDO =
            "SELECT COUNT(*) FROM entrega WHERE id_pedido = ?";

    /** Cuenta las entregas que dependen de un repartidor. */
    private static final String SQL_CONTAR_POR_REPARTIDOR =
            "SELECT COUNT(*) FROM entrega WHERE id_repartidor = ?";

    /** Inserción de una entrega nueva; el identificador lo genera MySQL. */
    private static final String SQL_INSERT =
            "INSERT INTO entrega (id_pedido, id_repartidor, fecha, hora) VALUES (?, ?, ?, ?)";

    /** Actualización de los datos de una entrega existente. */
    private static final String SQL_UPDATE =
            "UPDATE entrega SET id_pedido = ?, id_repartidor = ?, fecha = ?, hora = ? WHERE id = ?";

    /** Eliminación de una entrega por su identificador. */
    private static final String SQL_DELETE = "DELETE FROM entrega WHERE id = ?";

    /**
     * Guarda una entrega nueva y le asigna el identificador que generó MySQL.
     *
     * @param entrega entrega que se quiere insertar; al terminar queda con su
     *                identificador de base de datos
     * @return {@code true} si la entrega quedó guardada
     * @throws PersistenciaException si la base de datos rechaza la inserción
     */
    public boolean agregar(Entrega entrega) {
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement stmt =
                     conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            asignarDatos(stmt, entrega);
            if (stmt.executeUpdate() != 1) {
                return false;
            }

            try (ResultSet claves = stmt.getGeneratedKeys()) {
                if (claves.next()) {
                    entrega.setId(claves.getInt(1));
                }
            }
            return true;

        } catch (SQLIntegrityConstraintViolationException e) {
            // Situación esperada: el pedido o el repartidor ya no existen. Basta una
            // línea en la consola, porque la vista informa el problema al usuario.
            System.out.println("[EntregaDAO] MySQL rechazó insertar la entrega del pedido #"
                    + entrega.getIdPedido() + " por integridad referencial.");
            throw new PersistenciaException(
                    "No se pudo registrar la entrega: el pedido o el repartidor "
                            + "indicados no existen en la base de datos.", e);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new PersistenciaException(
                    "Error al guardar la entrega en la base de datos.", e);
        }
    }

    /**
     * Actualiza el pedido, el repartidor, la fecha y la hora de una entrega ya
     * existente.
     *
     * @param entrega entrega con los datos nuevos; su identificador indica la fila
     * @return {@code true} si se actualizó alguna fila; {@code false} si no existe
     *         una entrega con ese identificador
     * @throws PersistenciaException si la base de datos rechaza la actualización
     */
    public boolean editar(Entrega entrega) {
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {

            asignarDatos(stmt, entrega);
            stmt.setInt(5, entrega.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLIntegrityConstraintViolationException e) {
            // Situación esperada: el pedido o el repartidor ya no existen. Basta una
            // línea en la consola, porque la vista informa el problema al usuario.
            System.out.println("[EntregaDAO] MySQL rechazó actualizar la entrega #"
                    + entrega.getId() + " por integridad referencial.");
            throw new PersistenciaException(
                    "No se pudo actualizar la entrega: el pedido o el repartidor "
                            + "indicados no existen en la base de datos.", e);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new PersistenciaException(
                    "Error al actualizar la entrega en la base de datos.", e);
        }
    }

    /**
     * Elimina una entrega de la base de datos.
     *
     * @param id identificador de la entrega
     * @return {@code true} si se eliminó la fila; {@code false} si no existe una
     *         entrega con ese identificador
     * @throws PersistenciaException si la base de datos rechaza la eliminación
     */
    public boolean eliminar(int id) {
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {

            stmt.setInt(1, id);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new PersistenciaException(
                    "Error al eliminar la entrega #" + id + " en la base de datos.", e);
        }
    }

    /**
     * Lee todas las entregas guardadas en MySQL, con la dirección del pedido y el
     * nombre del repartidor.
     *
     * @return la lista de entregas leídas, vacía si la tabla no tiene filas
     * @throws PersistenciaException si la lectura falla
     */
    public List<Entrega> listarTodos() {
        List<Entrega> leidas = new ArrayList<>();

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                leidas.add(mapear(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new PersistenciaException(
                    "Error al cargar las entregas desde la base de datos.", e);
        }

        return leidas;
    }

    /**
     * Busca una entrega por su identificador.
     *
     * @param id identificador de la entrega
     * @return la entrega encontrada, o {@code null} si no existe
     * @throws PersistenciaException si la lectura falla
     */
    public Entrega buscarPorId(int id) {
        return buscarPor(SQL_SELECT_BY_ID, id,
                "Error al buscar la entrega #" + id + " en la base de datos.");
    }

    /**
     * Busca la entrega registrada para un pedido. Sirve para que el controlador
     * pueda comprobar si un pedido ya tiene su entrega registrada.
     *
     * @param idPedido identificador del pedido
     * @return la entrega de ese pedido, o {@code null} si el pedido no tiene una
     * @throws PersistenciaException si la lectura falla
     */
    public Entrega buscarPorPedido(int idPedido) {
        return buscarPor(SQL_SELECT_BY_PEDIDO, idPedido,
                "Error al buscar la entrega del pedido #" + idPedido
                        + " en la base de datos.");
    }

    /**
     * Cuenta las entregas registradas de un pedido. Sirve para que el controlador
     * de pedidos avise antes de intentar un borrado que la llave foránea va a
     * rechazar.
     *
     * @param idPedido identificador del pedido
     * @return la cantidad de entregas que dependen de ese pedido
     * @throws PersistenciaException si la lectura falla
     */
    public int contarPorPedido(int idPedido) {
        return contar(SQL_CONTAR_POR_PEDIDO, idPedido,
                "Error al contar las entregas del pedido #" + idPedido
                        + " en la base de datos.");
    }

    /**
     * Cuenta las entregas realizadas por un repartidor. Sirve para que el
     * controlador de repartidores avise antes de intentar un borrado que la llave
     * foránea va a rechazar.
     *
     * @param idRepartidor identificador del repartidor
     * @return la cantidad de entregas que dependen de ese repartidor
     * @throws PersistenciaException si la lectura falla
     */
    public int contarPorRepartidor(int idRepartidor) {
        return contar(SQL_CONTAR_POR_REPARTIDOR, idRepartidor,
                "Error al contar las entregas del repartidor con ID " + idRepartidor
                        + " en la base de datos.");
    }

    /**
     * Ejecuta una de las consultas {@code COUNT(*)} que reciben un único parámetro
     * entero.
     *
     * @param sql            consulta de conteo con un parámetro
     * @param parametro      valor del parámetro
     * @param mensajeDeError mensaje que se muestra si la lectura falla
     * @return el número contado, o {@code 0} si la consulta no devuelve filas
     * @throws PersistenciaException si la lectura falla
     */
    private int contar(String sql, int parametro, String mensajeDeError) {
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, parametro);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new PersistenciaException(mensajeDeError, e);
        }
    }

    /**
     * Ejecuta una de las consultas de búsqueda que reciben un único parámetro
     * entero, para no repetir el try-with-resources en cada una.
     *
     * @param sql            consulta con un parámetro
     * @param parametro      valor del parámetro
     * @param mensajeDeError mensaje que se muestra si la lectura falla
     * @return la primera entrega encontrada, o {@code null} si no hay ninguna
     * @throws PersistenciaException si la lectura falla
     */
    private Entrega buscarPor(String sql, int parametro, String mensajeDeError) {
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, parametro);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new PersistenciaException(mensajeDeError, e);
        }
    }

    /**
     * Deja en el {@link PreparedStatement} los cuatro datos que comparten la
     * inserción y la actualización, en el mismo orden.
     *
     * @param stmt    sentencia preparada
     * @param entrega entrega con los datos que se van a guardar
     * @throws SQLException si JDBC rechaza alguno de los valores
     */
    private void asignarDatos(PreparedStatement stmt, Entrega entrega) throws SQLException {
        stmt.setInt(1, entrega.getIdPedido());
        stmt.setInt(2, entrega.getIdRepartidor());
        stmt.setDate(3, Date.valueOf(entrega.getFecha()));
        stmt.setTime(4, Time.valueOf(entrega.getHora()));
    }

    /**
     * Convierte la fila actual del {@link ResultSet} en una {@link Entrega},
     * incluidos los datos que traen los JOIN para mostrar en la interfaz.
     *
     * @param rs resultado posicionado en la fila que se quiere leer
     * @return la entrega leída
     * @throws SQLException si alguna columna no se puede leer
     */
    private Entrega mapear(ResultSet rs) throws SQLException {
        Entrega entrega = new Entrega(
                rs.getInt("id"),
                rs.getInt("id_pedido"),
                rs.getInt("id_repartidor"),
                rs.getDate("fecha").toLocalDate(),
                rs.getTime("hora").toLocalTime());
        entrega.setDireccionPedido(rs.getString("direccion_pedido"));
        entrega.setNombreRepartidor(rs.getString("nombre_repartidor"));
        return entrega;
    }
}
