package cl.duocuc.app;

import cl.duocuc.model.Pedido;
import cl.duocuc.model.PedidoComida;
import cl.duocuc.model.PedidoEncomienda;
import cl.duocuc.model.PedidoExpress;

/**
 * Punto de entrada de la aplicación SpeedFast.
 * Demuestra el despacho, la cancelación y el rastreo de pedidos, junto con
 * el polimorfismo y la sobrecarga de métodos en la jerarquía de {@link Pedido}.
 */
public class Main {

    /** Línea usada para separar los distintos casos de la simulación. */
    private static final String SEPARADOR = "----------------------------------------";

    /**
     * Ejecuta la demostración completa de la aplicación.
     *
     * @param args argumentos de línea de comandos; no se utilizan
     */
    public static void main(String[] args) {

        PedidoComida comida         = new PedidoComida(101, "Av. Italia 456", 4, "La Pizzería", 15);
        PedidoEncomienda encomienda = new PedidoEncomienda(102, "Av. Santa Rosa 567", 7, 3.5, 8.0);
        PedidoExpress express       = new PedidoExpress(103, "Av. Apoquindo 1500", 7, "TechStore");

        ControladorDeEnvios controlador = new ControladorDeEnvios();

        System.out.println("[Despachando pedidos]\n");
        controlador.despachar(comida);
        controlador.despachar(encomienda);
        System.out.println(SEPARADOR + "\n");

        System.out.println("[Cancelando pedido]\n");
        controlador.cancelar(express);
        System.out.println(SEPARADOR + "\n");

        System.out.println("[Historial de entregas]\n");
        controlador.verHistorial();
        System.out.println("\n" + SEPARADOR + "\n");

        System.out.println("[Polimorfismo - resumen de pedidos]\n");
        Pedido[] pedidos = { comida, encomienda, express };
        for (Pedido p : pedidos) {
            p.mostrarResumen();
            System.out.println();
        }
        System.out.println(SEPARADOR + "\n");

        System.out.println("[Polimorfismo - asignarRepartidor()]\n");
        for (Pedido p : pedidos) {
            System.out.println(p.asignarRepartidor());
        }
        System.out.println("\n" + SEPARADOR + "\n");

        System.out.println("[Sobrecarga - asignarRepartidor(String nombre)]\n");
        System.out.println(comida.asignarRepartidor("Valentina Soto"));
        System.out.println(encomienda.asignarRepartidor("Pedro Núñez"));
        System.out.println(express.asignarRepartidor("Ignacio Fuentes"));
        System.out.println("\n" + SEPARADOR);
    }
}
