package cl.duocuc.app;

import cl.duocuc.model.Pedido;
import cl.duocuc.model.PedidoComida;
import cl.duocuc.model.PedidoEncomienda;
import cl.duocuc.model.PedidoExpress;

public class Main {
    public static void main(String[] args) {

        PedidoComida comida         = new PedidoComida(1, "Av. Italia 456", 4, "La Pizzería", 15);
        PedidoEncomienda encomienda = new PedidoEncomienda(22, "Av. Independencia 123", 6, 3.5, 8.0);
        PedidoExpress express       = new PedidoExpress(83, "Av. Apoquindo 1500", 7, "TechStore");

        System.out.println("=== Resumen de pedidos (mostrarResumen + calcularTiempoEntrega) ===\n");
        Pedido[] pedidos = { comida, encomienda, express };
        for (Pedido p : pedidos) {
            p.mostrarResumen();
            System.out.println();
        }

        System.out.println("=== Polimorfismo con asignarRepartidor() ===\n");
        for (Pedido p : pedidos) {
            System.out.println(p.asignarRepartidor());
        }

        System.out.println("\n=== Sobrecarga PedidoComida ===");
        System.out.println(comida.asignarRepartidor(false));
        System.out.println(comida.asignarRepartidor(true));

        System.out.println("\n=== Sobrecarga PedidoEncomienda ===");
        System.out.println(encomienda.asignarRepartidor(false));
        System.out.println(encomienda.asignarRepartidor(true));

        System.out.println("\n=== Sobrecarga PedidoExpress ===");
        System.out.println(express.asignarRepartidor(14));
        System.out.println(express.asignarRepartidor(18));

        System.out.println("\n=== Sobrecarga heredada asignarRepartidor(String tipo) ===");
        System.out.println(comida.asignarRepartidor("moto"));
        System.out.println(encomienda.asignarRepartidor("camioneta"));
        System.out.println(express.asignarRepartidor("bicicleta"));
    }
}
