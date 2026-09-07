package cl.duocuc.app;

import cl.duocuc.interfaces.Cancelable;
import cl.duocuc.interfaces.Despachable;
import cl.duocuc.interfaces.Rastreable;
import cl.duocuc.model.Pedido;

import java.util.ArrayList;

/**
 * Gestiona el ciclo de vida de los pedidos: los despacha, los cancela
 * y mantiene el historial de las entregas realizadas.
 */
public class ControladorDeEnvios implements Despachable, Cancelable, Rastreable {

    private ArrayList<String> historial = new ArrayList<>();
    private Pedido pedidoActual;

    /**
     * Define el pedido sobre el que operarán los métodos sin parámetros.
     *
     * @param pedido pedido a dejar en curso
     */
    public void setPedidoActual(Pedido pedido) {
        this.pedidoActual = pedido;
    }

    /**
     * Despacha el pedido en curso: le asigna un repartidor, muestra su resumen
     * y registra la entrega en el historial. No hace nada si no hay pedido en curso.
     */
    @Override
    public void despachar() {
        if (pedidoActual == null) return;
        String detalleAsignacion = pedidoActual.asignarRepartidor();
        historial.add(pedidoActual.getClass().getSimpleName()
                + " #" + String.format("%03d", pedidoActual.getIdPedido())
                + " - entregado por " + pedidoActual.getRepartidor());
        System.out.println("[" + pedidoActual.getClass().getSimpleName() + "]");
        pedidoActual.mostrarResumen();
        System.out.println(detalleAsignacion);
        System.out.println("Pedido despachado correctamente.\n");
    }

    /**
     * Cancela el pedido en curso e informa el resultado en consola.
     * No hace nada si no hay pedido en curso.
     */
    @Override
    public void cancelar() {
        if (pedidoActual == null) return;
        System.out.println("Cancelando " + pedidoActual.getClass().getSimpleName()
                + " #" + String.format("%03d", pedidoActual.getIdPedido()) + "...");
        System.out.println("--> Pedido cancelado exitosamente.\n");
    }

    /**
     * Imprime en consola el historial de los pedidos despachados.
     */
    @Override
    public void verHistorial() {
        System.out.println("Historial:");
        for (String entrada : historial) {
            System.out.println("- " + entrada);
        }
    }

    /**
     * Deja el pedido indicado en curso y lo despacha.
     *
     * @param pedido pedido a despachar
     */
    public void despachar(Pedido pedido) {
        setPedidoActual(pedido);
        despachar();
    }

    /**
     * Deja el pedido indicado en curso y lo cancela.
     *
     * @param pedido pedido a cancelar
     */
    public void cancelar(Pedido pedido) {
        setPedidoActual(pedido);
        cancelar();
    }
}
