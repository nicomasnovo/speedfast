package cl.duocuc.conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Punto único de conexión con MySQL. Centraliza la configuración JDBC (URL,
 * usuario y contraseña) y la comprobación de que la base de datos responde.
 * <p>
 * Los DAO son los únicos que piden conexiones con {@link #obtenerConexion()}; el
 * resto de la aplicación no conoce esta clase, salvo {@code Main}, que al arrancar
 * valida la conexión con {@link #validarConexion()}.
 */
public class ConexionBD {
    private static final String URL = "jdbc:mysql://localhost:3306/speedfast";
    private static final String USUARIO = "root";
    private static final String CONTRASENA = "MysqlDuoc#2026";

    /**
     * Abre una conexión nueva con la base de datos.
     *
     * @return la conexión abierta; quien la pide debe cerrarla
     * @throws SQLException si la base de datos no responde o rechaza las credenciales
     */
    public static Connection obtenerConexion() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, CONTRASENA);
    }

    /**
     * Comprueba que la base de datos esté disponible al iniciar la aplicación.
     * Reutiliza {@link #obtenerConexion()}, por lo que no duplica la
     * configuración JDBC, e informa el resultado por consola igual que antes lo
     * hacía {@code Main}.
     *
     * @return {@code true} si se pudo abrir una conexión utilizable
     */
    public static boolean validarConexion() {
        try (Connection conn = obtenerConexion()) {
            boolean valida = conn != null && !conn.isClosed();
            if (valida) {
                System.out.println("✅ Conexión exitosa a la base de datos.");
            } else {
                System.err.println("❌ Error al conectar con la base de datos: "
                        + "la conexión no quedó abierta.");
            }
            return valida;
        } catch (SQLException e) {
            System.err.println("❌ Error al conectar con la base de datos:");
            e.printStackTrace();
            return false;
        }
    }
}
