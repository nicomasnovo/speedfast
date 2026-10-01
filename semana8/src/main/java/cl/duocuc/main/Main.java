package cl.duocuc.main;

import cl.duocuc.conexion.ConexionBD;
import cl.duocuc.controllers.EntregaController;
import cl.duocuc.controllers.PedidoController;
import cl.duocuc.controllers.RepartidorController;
import cl.duocuc.exception.PersistenciaException;
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

    /**
     * Crea los controladores, carga los datos desde la base de datos y muestra la
     * interfaz Swing.
     *
     * @param args argumentos de línea de comandos; no se utilizan
     */
    public static void main(String[] args) {

        if (!ConexionBD.validarConexion()) {
            System.err.println("La aplicación se iniciará sin conexión confirmada.");
        }

        ZonaDeCarga zonaDeCarga = new ZonaDeCarga();
        PedidoController pedidoController = new PedidoController(zonaDeCarga);
        RepartidorController repartidorController = new RepartidorController(zonaDeCarga);
        EntregaController entregaController = new EntregaController(pedidoController, repartidorController);

        try {
            int repartidoresCargados = repartidorController.cargarRepartidoresDesdeDB();
            int pedidosCargados = pedidoController.cargarPedidosDesdeDB();
            System.out.println("Cargados desde MySQL: " + repartidoresCargados + " repartidor(es) y " + pedidosCargados + " pedido(s).");
        } catch (PersistenciaException e) {
            System.err.println("No se pudieron cargar los datos iniciales: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> new VentanaPrincipal(pedidoController, repartidorController, entregaController).setVisible(true));
    }
}
