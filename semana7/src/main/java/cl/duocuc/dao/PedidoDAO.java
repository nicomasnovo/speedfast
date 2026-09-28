package cl.duocuc.dao;

import cl.duocuc.conexion.ConexionBD;
import cl.duocuc.model.EstadoPedido;
import cl.duocuc.model.Pedido;
import cl.duocuc.model.TipoPedido;

import javax.swing.JOptionPane;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de la tabla {@code pedido}. Concentra el CRUD y todo el SQL de
 * los pedidos, que antes vivía en {@code PedidoController}: el controlador ahora
 * solo coordina, y es este DAO el único que usa {@link Connection},
 * {@link PreparedStatement} y {@link ResultSet}.
 * <p>
 * Las conexiones se piden siempre a {@link ConexionBD#obtenerConexion()} y se
 * cierran con try-with-resources. El flujo completo es
 * vista → controlador → DAO → ConexionBD → MySQL.
 */
public class PedidoDAO {

    //Se crean las variables SQL para mejorar la legibilidad de las clases creadas
    /** Consulta que trae todos los pedidos guardados. */
    private static final String SQL_SELECT =
            "SELECT id, direccion, tipo, estado, repartidor FROM pedido ORDER BY id";

    /**
     * Inserción de un pedido nuevo. No incluye el repartidor porque un pedido
     * recién registrado todavía no tiene uno asignado.
     */
    private static final String SQL_INSERT =
            "INSERT INTO pedido (id, direccion, tipo, estado) VALUES (?, ?, ?, ?)";

    /** Actualización de los datos de un pedido existente. */
    private static final String SQL_UPDATE =
            "UPDATE pedido SET direccion = ?, tipo = ?, estado = ?, repartidor = ? WHERE id = ?";

    /** Eliminación de un pedido por su identificador. */
    private static final String SQL_DELETE = "DELETE FROM pedido WHERE id = ?";

    /**
     * Guarda un pedido en la base de datos mediante un {@link PreparedStatement}.
     *
     * @param pedido pedido que se quiere insertar
     * @return {@code true} si el pedido quedó guardado
     */
    public boolean agregar(Pedido pedido) {
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT)) {

            stmt.setInt(1, pedido.getId());
            stmt.setString(2, pedido.getDireccionEntrega());
            stmt.setString(3, pedido.getTipo().name());
            stmt.setString(4, pedido.getEstado().name());

            return stmt.executeUpdate() == 1;

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "Error al guardar el pedido en la base de datos.");
            return false;
        }
    }

    /**
     * Actualiza en la base de datos la dirección, el tipo, el estado y el
     * repartidor de un pedido ya existente. También se usa para guardar la
     * asignación hecha desde la interfaz, porque esa asignación solo cambia el
     * repartidor del pedido.
     *
     * @param pedido pedido con los datos nuevos; su identificador indica la fila
     * @return {@code true} si se actualizó alguna fila
     */
    public boolean editar(Pedido pedido) {
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {

            stmt.setString(1, pedido.getDireccionEntrega());
            stmt.setString(2, pedido.getTipo().name());
            stmt.setString(3, pedido.getEstado().name());
            // Queda NULL en la base de datos mientras el pedido no tenga repartidor.
            stmt.setString(4, pedido.getRepartidorAsignado());
            stmt.setInt(5, pedido.getId());

            int filas = stmt.executeUpdate();
            if (filas == 0) {
                JOptionPane.showMessageDialog(
                        null,
                        "No existe el pedido #" + pedido.getId() + " en la base de datos.");
                return false;
            }
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "Error al actualizar el pedido en la base de datos.");
            return false;
        }
    }

    /**
     * Elimina un pedido de la base de datos.
     *
     * @param id identificador del pedido que se quiere eliminar
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
                        "No existe el pedido #" + id + " en la base de datos.");
                return false;
            }
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "No se pudo eliminar el pedido #" + id
                            + ": tiene entregas asociadas o hubo un error en la base de datos.");
            return false;
        }
    }

    /**
     * Lee todos los pedidos guardados en MySQL y los convierte en objetos
     * {@link Pedido}.
     *
     * @return la lista de pedidos leídos, vacía si la tabla no tiene filas, o
     *         {@code null} si la lectura falló (en ese caso el error ya se
     *         informó al usuario). Se distingue el fallo de la lista vacía para
     *         que el controlador no borre los pedidos que tiene en memoria
     *         cuando la base de datos no responde.
     */
    public List<Pedido> cargarDesdeDB() {
        List<Pedido> leidos = new ArrayList<>();

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Pedido pedido = new Pedido(
                        rs.getInt("id"),
                        rs.getString("direccion"),
                        TipoPedido.valueOf(rs.getString("tipo").trim().toUpperCase()),
                        EstadoPedido.valueOf(rs.getString("estado").trim().toUpperCase()));
                // getString devuelve null si la columna está en NULL, es decir,
                // si el pedido todavía no tiene repartidor asignado.
                pedido.setRepartidorAsignado(rs.getString("repartidor"));
                leidos.add(pedido);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "Error al cargar los pedidos desde la base de datos.");
            return null;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "La base de datos tiene un tipo o estado de pedido no válido: "
                            + e.getMessage());
            return null;
        }

        return leidos;
    }

    /**
     * Guarda en la base de datos el estado y el repartidor actuales de los
     * pedidos recibidos. Se usa al terminar el reparto, para que las entregas
     * hechas por los hilos de los repartidores queden persistidas junto con el
     * repartidor que se hizo cargo de cada pedido.
     *
     * @param pedidos pedidos que se quieren actualizar
     * @return la cantidad de pedidos actualizados
     */
    public int guardarEstados(List<Pedido> pedidos) {
        int actualizados = 0;
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {

            for (Pedido pedido : pedidos) {
                stmt.setString(1, pedido.getDireccionEntrega());
                stmt.setString(2, pedido.getTipo().name());
                stmt.setString(3, pedido.getEstado().name());
                stmt.setString(4, pedido.getRepartidorAsignado());
                stmt.setInt(5, pedido.getId());
                actualizados += stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "Error al actualizar el estado de los pedidos en la base de datos.");
        }
        return actualizados;
    }
}
