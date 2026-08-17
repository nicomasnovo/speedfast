package cl.duocuc.app;

import cl.duocuc.model.Pedido;
import cl.duocuc.model.PedidoComida;
import cl.duocuc.model.PedidoEncomienda;
import cl.duocuc.model.PedidoExpress;

public class Main {
    public static void main(String[] args) {

        PedidoComida comida = new PedidoComida("P001", "Ana Torres", "Av. Libertad 123","La Pizzería", 25);
        PedidoEncomienda encomienda = new PedidoEncomienda("P002", "Luis Pérez", "Calle 5 Norte 456",3.5, 8.0);
        PedidoExpress express = new PedidoExpress("P003", "María Soto", "Pasaje Roble 789","TechStore", 12.4);

        System.out.println("=== Polimorfismo con asignarRepartidor() ===");
        Pedido[] pedidos = { comida, encomienda, express };

        for (Pedido p : pedidos) {
            System.out.println(p);
            System.out.println("  -> " + p.asignarRepartidor());
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

        System.out.println("\n=== Sobrecarga heredada asignarRepartidor(String) ===");
        System.out.println(comida.asignarRepartidor("moto"));
        System.out.println(encomienda.asignarRepartidor("camioneta"));
        System.out.println(express.asignarRepartidor("bicicleta"));
    }
}
