package cl.duocuc.dao;

import cl.duocuc.conexion.ConexionBD;
import cl.duocuc.exception.PersistenciaException;
import cl.duocuc.model.EstadoPedido;
import cl.duocuc.model.Pedido;
import cl.duocuc.model.TipoPedido;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
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
 * <p>
 * Este DAO ya no depende de Swing: cuando una operación falla lanza una
 * {@link PersistenciaException} con un mensaje entendible, y es la vista la que
 * lo muestra con {@code JOptionPane}. Cuando la operación se ejecuta bien pero no
 * afecta ninguna fila (por ejemplo, editar un pedido que ya no existe), devuelve
 * {@code false} para que el controlador decida qué informar.
 */
public class PedidoDAO {

    //Se crean las variables SQL para mejorar la legibilidad de las clases creadas
    /** Consulta que trae todos los pedidos guardados. */
    private static final String SQL_SELECT =
            "SELECT id, direccion, tipo, estado, repartidor FROM pedido ORDER BY id";

    /** Consulta que trae un pedido por su identificador. */
    private static final String SQL_SELECT_BY_ID =
            "SELECT id, direccion, tipo, estado, repartidor FROM pedido WHERE id = ?";

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
     * @throws PersistenciaException si la base de datos rechaza la inserción
     */
    public boolean agregar(Pedido pedido) {
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT)) {

            stmt.setInt(1, pedido.getId());
            stmt.setString(2, pedido.getDireccionEntrega());
            stmt.setString(3, pedido.getTipo().name());
            stmt.setString(4, pedido.getEstado().name());

            return stmt.executeUpdate() == 1;

        } catch (SQLIntegrityConstraintViolationException e) {
            // El ID ya está ocupado: es un dato mal ingresado, no un fallo técnico.
            System.out.println("[PedidoDAO] MySQL rechazó insertar el pedido #"
                    + pedido.getId() + ": la clave primaria ya existe.");
            throw new PersistenciaException(
                    "Ya existe un pedido con el ID " + pedido.getId()
                            + " en la base de datos.", e);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new PersistenciaException(
                    "Error al guardar el pedido en la base de datos.", e);
        }
    }

    /**
     * Actualiza en la base de datos la dirección, el tipo, el estado y el
     * repartidor de un pedido ya existente. También se usa para guardar la
     * asignación hecha desde la interfaz, porque esa asignación solo cambia el
     * repartidor del pedido.
     *
     * @param pedido pedido con los datos nuevos; su identificador indica la fila
     * @return {@code true} si se actualizó alguna fila; {@code false} si no existe
     *         un pedido con ese identificador
     * @throws PersistenciaException si la base de datos rechaza la actualización
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

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new PersistenciaException(
                    "Error al actualizar el pedido #" + pedido.getId()
                            + " en la base de datos.", e);
        }
    }

    /**
     * Elimina un pedido de la base de datos.
     *
     * @param id identificador del pedido que se quiere eliminar
     * @return {@code true} si se eliminó la fila; {@code false} si no existe un
     *         pedido con ese identificador
     * @throws PersistenciaException si la base de datos rechaza la eliminación,
     *                               por ejemplo porque el pedido tiene entregas
     *                               registradas (integridad referencial)
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
            System.out.println("[PedidoDAO] MySQL rechazó eliminar el pedido #" + id
                    + " por integridad referencial.");
            throw new PersistenciaException(
                    "No se puede eliminar el pedido #" + id
                            + " porque tiene entregas registradas. "
                            + "Elimine primero sus entregas.", e);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new PersistenciaException(
                    "Error al eliminar el pedido #" + id + " en la base de datos.", e);
        }
    }

    /**
     * Lee todos los pedidos guardados en MySQL y los convierte en objetos
     * {@link Pedido}.
     *
     * @return la lista de pedidos leídos, vacía si la tabla no tiene filas
     * @throws PersistenciaException si la lectura falla. Al lanzar la excepción en
     *                               vez de devolver una lista vacía, el
     *                               controlador no alcanza a vaciar la zona de
     *                               carga cuando la base de datos no responde.
     */
    public List<Pedido> cargarDesdeDB() {
        List<Pedido> leidos = new ArrayList<>();

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                leidos.add(mapear(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new PersistenciaException(
                    "Error al cargar los pedidos desde la base de datos.", e);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            throw new PersistenciaException(
                    "La base de datos tiene un tipo o estado de pedido no válido: "
                            + e.getMessage(), e);
        }

        return leidos;
    }

    /**
     * Busca un pedido en la base de datos por su identificador. El controlador lo
     * usa para saber si un identificador ya está ocupado antes de registrar un
     * pedido nuevo, y para comprobar que el pedido de una entrega existe.
     *
     * @param id identificador del pedido
     * @return el pedido encontrado, o {@code null} si no existe
     * @throws PersistenciaException si la lectura falla
     */
    public Pedido buscarPorId(int id) {
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BY_ID)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new PersistenciaException(
                    "Error al buscar el pedido #" + id + " en la base de datos.", e);
        }
    }

    /**
     * Convierte la fila actual del {@link ResultSet} en un {@link Pedido}. Lo
     * comparten {@link #cargarDesdeDB()} y {@link #buscarPorId(int)} para no
     * repetir la lectura de columnas.
     *
     * @param rs resultado posicionado en la fila que se quiere leer
     * @return el pedido leído
     * @throws SQLException             si alguna columna no se puede leer
     * @throws IllegalArgumentException si el tipo o el estado guardados no son válidos
     */
    private Pedido mapear(ResultSet rs) throws SQLException {
        Pedido pedido = new Pedido(
                rs.getInt("id"),
                rs.getString("direccion"),
                TipoPedido.valueOf(rs.getString("tipo").trim().toUpperCase()),
                EstadoPedido.valueOf(rs.getString("estado").trim().toUpperCase()));
        // getString devuelve null si la columna está en NULL, es decir,
        // si el pedido todavía no tiene repartidor asignado.
        pedido.setRepartidorAsignado(rs.getString("repartidor"));
        return pedido;
    }

    /**
     * Guarda en la base de datos el estado y el repartidor actuales de los
     * pedidos recibidos. Se usa al terminar el reparto, para que las entregas
     * hechas por los hilos de los repartidores queden persistidas junto con el
     * repartidor que se hizo cargo de cada pedido.
     *
     * @param pedidos pedidos que se quieren actualizar
     * @return la cantidad de pedidos actualizados
     * @throws PersistenciaException si la base de datos rechaza la actualización
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
            throw new PersistenciaException(
                    "Error al actualizar el estado de los pedidos en la base de datos.", e);
        }
        return actualizados;
    }
}
