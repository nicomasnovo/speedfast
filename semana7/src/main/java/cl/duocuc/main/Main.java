package cl.duocuc.main;

import cl.duocuc.controllers.PedidoController;
import cl.duocuc.controllers.RepartidorController;
import cl.duocuc.model.TipoPedido;
import cl.duocuc.model.ZonaDeCarga;
import cl.duocuc.view.VentanaPrincipal;

import javax.swing.SwingUtilities;

/**
 * Punto de entrada de la aplicación SpeedFast.
 * <p>
 * Su única responsabilidad es inicializar la aplicación: crea el almacenamiento
 * compartido ({@link ZonaDeCarga}), los controladores y la ventana principal. Los
 * controladores se crean una sola vez y se comparten con todas las vistas, de modo
 * que trabajen siempre sobre los mismos pedidos.
 */
public class Main {

    /** Nombres de los repartidores; hay un hilo por cada uno al repartir. */
    private static final String[] NOMBRES_REPARTIDORES = {"Camila", "Luis", "Pedro"};

    /**
     * Inicializa los datos, los controladores y muestra la interfaz Swing.
     *
     * @param args argumentos de línea de comandos; no se utilizan
     */
    public static void main(String[] args) {

        ZonaDeCarga zonaDeCarga = new ZonaDeCarga();
        PedidoController pedidoController = new PedidoController(zonaDeCarga);
        RepartidorController repartidorController =
                new RepartidorController(zonaDeCarga, NOMBRES_REPARTIDORES);

        cargarPedidosIniciales(pedidoController);

        SwingUtilities.invokeLater(() ->
                new VentanaPrincipal(pedidoController, repartidorController).setVisible(true));
    }

    /**
     * Deja en la zona de carga los pedidos con que parte la simulación.
     *
     * @param pedidoController controlador que registra los pedidos
     */
    private static void cargarPedidosIniciales(PedidoController pedidoController) {
        pedidoController.registrarPedido(101, "Av. Italia 456", TipoPedido.COMIDA);
        pedidoController.registrarPedido(102, "Av. Apoquindo 1500", TipoPedido.ENCOMIENDA);
        pedidoController.registrarPedido(103, "Av. Santa Rosa 567", TipoPedido.EXPRESS);
        pedidoController.registrarPedido(104, "Los Leones 2100", TipoPedido.COMIDA);
        pedidoController.registrarPedido(105, "Providencia 890", TipoPedido.ENCOMIENDA);
        pedidoController.registrarPedido(106, "San Pablo 3200", TipoPedido.EXPRESS);
    }
}
