package cl.duocuc.dao;

import cl.duocuc.conexion.ConexionBD;
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

/**
 * Acceso a datos de la tabla {@code repartidor}. Concentra el CRUD y todo el SQL
 * de los repartidores, que antes vivía en {@code RepartidorController}: el
 * controlador ahora solo coordina el reparto, y es este DAO el único que usa
 * {@link Connection}, {@link PreparedStatement} y {@link ResultSet}.
 * <p>
 * Las conexiones se piden siempre a {@link ConexionBD#obtenerConexion()} y se
 * cierran con try-with-resources.
 */
public class RepartidorDAO {

    //Se crean las variables SQL para mejorar la legibilidad de las clases creadas
    /** Consulta que trae todos los repartidores guardados. */
    private static final String SQL_SELECT = "SELECT id, nombre FROM repartidor ORDER BY id";

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
     * @return la lista de repartidores leídos, vacía si la tabla no tiene filas,
     *         o {@code null} si la lectura falló (en ese caso el error ya se
     *         informó al usuario). Se distingue el fallo de la lista vacía para
     *         que el controlador no borre los repartidores que tiene en memoria
     *         cuando la base de datos no responde.
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
            JOptionPane.showMessageDialog(
                    null,
                    "Error al cargar los repartidores desde la base de datos.");
            return null;
        }

        return leidos;
    }

    /**
     * Guarda un repartidor nuevo y le asigna el identificador que generó MySQL.
     *
     * @param repartidor repartidor que se quiere insertar; al terminar queda con
     *                   su identificador de base de datos
     * @return {@code true} si el repartidor quedó guardado
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
            JOptionPane.showMessageDialog(
                    null,
                    "Error al guardar el repartidor en la base de datos.");
            return false;
        }
    }

    /**
     * Cambia el nombre de un repartidor en la base de datos.
     *
     * @param id     identificador del repartidor
     * @param nombre nuevo nombre del repartidor
     * @return {@code true} si se actualizó alguna fila
     */
    public boolean editar(int id, String nombre) {
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {

            stmt.setString(1, nombre);
            stmt.setInt(2, id);

            int filas = stmt.executeUpdate();
            if (filas == 0) {
                JOptionPane.showMessageDialog(
                        null,
                        "No existe el repartidor con ID " + id + " en la base de datos.");
                return false;
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
     * Elimina un repartidor de la base de datos.
     *
     * @param id identificador del repartidor
     * @return {@code true} si se eliminó la fila
     */
    public boolean eliminar(int id) {
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
}
