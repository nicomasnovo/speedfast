package cl.duocuc.main;

import cl.duocuc.controllers.ConexionBD;
import cl.duocuc.controllers.PedidoController;
import cl.duocuc.controllers.RepartidorController;
import cl.duocuc.model.TipoPedido;
import cl.duocuc.model.ZonaDeCarga;
import cl.duocuc.view.VentanaPrincipal;

import javax.swing.SwingUtilities;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Punto de entrada de la aplicación SpeedFast.
 * <p>
 * Su única responsabilidad es inicializar la aplicación: crea el almacenamiento
 * compartido ({@link ZonaDeCarga}), los controladores y la ventana principal. Los
 * controladores se crean una sola vez y se comparten con todas las vistas, de modo
 * que trabajen siempre sobre los mismos pedidos.
 * <p>
 * Desde la semana 7 los datos iniciales ya no se crean desde Java: los pedidos y
 * los repartidores se cargan desde MySQL y provienen del script
 * {@code db/script_estructura.sql}.
 */
public class Main {

    /*
     * Nombres de los repartidores con que antes partía la simulación. Se conservan
     * como referencia: hoy los repartidores se cargan desde la tabla repartidor,
     * con los datos insertados en db/script_estructura.sql.
     */
    // private static final String[] NOMBRES_REPARTIDORES = {"Camila", "Luis", "Pedro"};

    /**
     * Crea los controladores, carga los datos desde la base de datos y muestra la
     * interfaz Swing.
     *
     * @param args argumentos de línea de comandos; no se utilizan
     */
    public static void main(String[] args) {

        try (Connection conn = ConexionBD.obtenerConexion()) {
            System.out.println("✅ Conexión exitosa a la base de datos.");
        } catch (SQLException e) {
            System.err.println("❌ Error al conectar con la base de datos:");
            e.printStackTrace();
        }
        ZonaDeCarga zonaDeCarga = new ZonaDeCarga();
        PedidoController pedidoController = new PedidoController(zonaDeCarga);
        RepartidorController repartidorController = new RepartidorController(zonaDeCarga);

        // Los datos iniciales ahora se cargan desde db/script_estructura.sql,
        // por lo que ya no se crean pedidos ni repartidores desde Java.
        // cargarPedidosIniciales(pedidoController);

        int repartidoresCargados = repartidorController.cargarRepartidoresDesdeDB();
        int pedidosCargados = pedidoController.cargarPedidosDesdeDB();
        System.out.println("Cargados desde MySQL: " + repartidoresCargados
                + " repartidor(es) y " + pedidosCargados + " pedido(s).");

        SwingUtilities.invokeLater(() ->
                new VentanaPrincipal(pedidoController, repartidorController).setVisible(true));
    }
}
