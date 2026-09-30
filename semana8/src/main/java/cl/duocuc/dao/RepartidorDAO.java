package cl.duocuc.dao;

import cl.duocuc.conexion.ConexionBD;
import cl.duocuc.exception.PersistenciaException;
import cl.duocuc.model.Repartidor;
import cl.duocuc.model.ZonaDeCarga;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de la tabla {@code repartidor}. Concentra el CRUD y todo el SQL
 * de los repartidores, que antes vivía en {@code RepartidorController}: el
 * controlador ahora solo coordina el reparto, y es este DAO el único que usa
 * {@link Connection}, {@link PreparedStatement} y {@link ResultSet}.
 * <p>
 * Las conexiones se piden siempre a {@link ConexionBD#obtenerConexion()} y se
 * cierran con try-with-resources.
 * <p>
 * Este DAO ya no depende de Swing: cuando una operación falla lanza una
 * {@link PersistenciaException} con un mensaje entendible, y es la vista la que
 * lo muestra con {@code JOptionPane}.
 */
public class RepartidorDAO {

    //Se crean las variables SQL para mejorar la legibilidad de las clases creadas
    /** Consulta que trae todos los repartidores guardados. */
    private static final String SQL_SELECT = "SELECT id, nombre FROM repartidor ORDER BY id";

    /** Consulta que trae un repartidor por su identificador. */
    private static final String SQL_SELECT_BY_ID =
            "SELECT id, nombre FROM repartidor WHERE id = ?";

    /** Inserción de un repartidor nuevo; el identificador lo genera MySQL. */
    private static final String SQL_INSERT = "INSERT INTO repartidor (nombre) VALUES (?)";

    /** Actualización del nombre de un repartidor existente. */
    private static final String SQL_UPDATE = "UPDATE repartidor SET nombre = ? WHERE id = ?";

    /** Eliminación de un repartidor por su identificador. */
    private static final String SQL_DELETE = "DELETE FROM repartidor WHERE id = ?";

    /**
     * Lee todos los repartidores guardados en MySQL y los convierte en objetos
     * {@link Repartidor}.
     *
     * @param zonaDeCarga zona de carga compartida que necesita cada repartidor
     *                    para retirar sus pedidos; el modelo no existe sin ella
     * @return la lista de repartidores leídos, vacía si la tabla no tiene filas
     * @throws PersistenciaException si la lectura falla. Al lanzar la excepción en
     *                               vez de devolver una lista vacía, el
     *                               controlador no alcanza a vaciar su lista en
     *                               memoria cuando la base de datos no responde.
     */
    public List<Repartidor> cargarDesdeDB(ZonaDeCarga zonaDeCarga) {
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
            throw new PersistenciaException(
                    "Error al cargar los repartidores desde la base de datos.", e);
        }

        return leidos;
    }

    /**
     * Busca un repartidor en la base de datos por su identificador. El controlador
     * lo usa para comprobar que el repartidor de una entrega existe realmente
     * antes de intentar guardarla.
     *
     * @param id          identificador del repartidor
     * @param zonaDeCarga zona de carga compartida que necesita el modelo
     * @return el repartidor encontrado, o {@code null} si no existe
     * @throws PersistenciaException si la lectura falla
     */
    public Repartidor buscarPorId(int id, ZonaDeCarga zonaDeCarga) {
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BY_ID)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new Repartidor(rs.getInt("id"), rs.getString("nombre"), zonaDeCarga);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new PersistenciaException(
                    "Error al buscar el repartidor con ID " + id + " en la base de datos.", e);
        }
    }

    /**
     * Guarda un repartidor nuevo y le asigna el identificador que generó MySQL.
     *
     * @param repartidor repartidor que se quiere insertar; al terminar queda con
     *                   su identificador de base de datos
     * @return {@code true} si el repartidor quedó guardado
     * @throws PersistenciaException si la base de datos rechaza la inserción
     */
    public boolean agregar(Repartidor repartidor) {
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement stmt =
                     conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, repartidor.getNombre());
            if (stmt.executeUpdate() != 1) {
                return false;
            }

            try (ResultSet claves = stmt.getGeneratedKeys()) {
                if (claves.next()) {
                    repartidor.setId(claves.getInt(1));
                }
            }
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new PersistenciaException(
                    "Error al guardar el repartidor en la base de datos.", e);
        }
    }

    /**
     * Cambia el nombre de un repartidor en la base de datos.
     *
     * @param id     identificador del repartidor
     * @param nombre nuevo nombre del repartidor
     * @return {@code true} si se actualizó alguna fila; {@code false} si no existe
     *         un repartidor con ese identificador
     * @throws PersistenciaException si la base de datos rechaza la actualización
     */
    public boolean editar(int id, String nombre) {
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {

            stmt.setString(1, nombre);
            stmt.setInt(2, id);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new PersistenciaException(
                    "Error al actualizar el repartidor con ID " + id
                            + " en la base de datos.", e);
        }
    }

    /**
     * Elimina un repartidor de la base de datos.
     *
     * @param id identificador del repartidor
     * @return {@code true} si se eliminó la fila; {@code false} si no existe un
     *         repartidor con ese identificador
     * @throws PersistenciaException si la base de datos rechaza la eliminación,
     *                               por ejemplo porque el repartidor tiene
     *                               entregas registradas (integridad referencial)
     */
    public boolean eliminar(int id) {
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {

            stmt.setInt(1, id);

            return stmt.executeUpdate() > 0;

        } catch (SQLIntegrityConstraintViolationException e) {
            // Situación esperada: MySQL rechaza el borrado por la llave foránea de
            // la tabla entrega. Se deja una línea en la consola en vez de la traza
            // completa, porque no es un fallo de la aplicación: el controlador la
            // propaga y la vista se lo explica al usuario.
            System.out.println("[RepartidorDAO] MySQL rechazó eliminar el repartidor con ID "
                    + id + " por integridad referencial.");
            throw new PersistenciaException(
                    "No se puede eliminar el repartidor con ID " + id
                            + " porque tiene entregas registradas. "
                            + "Elimine primero sus entregas.", e);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new PersistenciaException(
                    "Error al eliminar el repartidor con ID " + id
                            + " en la base de datos.", e);
        }
    }
}
