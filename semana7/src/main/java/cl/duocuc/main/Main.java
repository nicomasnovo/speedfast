package cl.duocuc.main;

import cl.duocuc.conexion.ConexionBD;
import cl.duocuc.controllers.PedidoController;
import cl.duocuc.controllers.RepartidorController;
import cl.duocuc.model.ZonaDeCarga;
import cl.duocuc.view.VentanaPrincipal;

import javax.swing.SwingUtilities;

/**
 * Punto de entrada de la aplicación SpeedFast.
 * <p>
 * Su única responsabilidad es inicializar la aplicación: pide a
 * {@link ConexionBD#validarConexion()} que compruebe la base de datos y luego crea
 * el almacenamiento compartido ({@link ZonaDeCarga}), los controladores y la
 * ventana principal. Los controladores se crean una sola vez y se comparten con
 * todas las vistas, de modo que trabajen siempre sobre los mismos pedidos.
 * <p>
 * Esta clase no usa JDBC: no conoce {@code Connection} ni SQL, solo el resultado
 * de la validación.
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

        if (!ConexionBD.validarConexion()) {
            // Se conserva el comportamiento anterior: la interfaz se abre igual,
            // porque cada operación vuelve a pedir su conexión y avisa al usuario
            // si la base de datos sigue sin responder.
            System.err.println("La aplicación se iniciará sin conexión confirmada.");
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
