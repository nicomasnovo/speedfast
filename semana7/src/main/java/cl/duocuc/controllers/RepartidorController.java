package cl.duocuc.controllers;

import cl.duocuc.model.Pedido;
import cl.duocuc.model.Repartidor;
import cl.duocuc.model.ZonaDeCarga;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Controlador de repartidores: asigna pedidos y coordina el reparto concurrente
 * ejecutando cada {@link Repartidor} en su propio hilo dentro de un
 * {@link ExecutorService}, tal como lo hacía la versión de consola.
 * <p>
 * El método {@link #iniciarEntregas()} es bloqueante porque espera a que los
 * repartidores terminen, por lo que la vista debe llamarlo fuera del Event
 * Dispatch Thread (por ejemplo, con un {@code SwingWorker}).
 */
public class RepartidorController {

    /** Tiempo máximo de espera para que el pool termine las entregas. */
    private static final int ESPERA_MAXIMA_SEGUNDOS = 60;

    private final ZonaDeCarga zonaDeCarga;
    private final List<Repartidor> repartidores = new ArrayList<>();

    /** Indica si hay un reparto en ejecución; se consulta desde la interfaz. */
    private volatile boolean entregasEnCurso;

    /**
     * Crea el controlador con un repartidor por cada nombre recibido.
     *
     * @param zonaDeCarga zona de carga compartida desde la que retiran los pedidos
     * @param nombres     nombres de los repartidores disponibles
     */
    public RepartidorController(ZonaDeCarga zonaDeCarga, String... nombres) {
        this.zonaDeCarga = zonaDeCarga;
        for (String nombre : nombres) {
            repartidores.add(new Repartidor(nombre, zonaDeCarga));
        }
    }

    /**
     * Obtiene los repartidores del sistema.
     *
     * @return la lista de repartidores
     */
    public List<Repartidor> getRepartidores() { return List.copyOf(repartidores); }

    /**
     * Obtiene los nombres de los repartidores, para mostrarlos en la vista.
     *
     * @return la lista de nombres
     */
    public List<String> getNombresRepartidores() {
        List<String> nombres = new ArrayList<>();
        for (Repartidor repartidor : repartidores) {
            nombres.add(repartidor.getNombre());
        }
        return nombres;
    }

    /**
     * Asigna un pedido pendiente a un repartidor, reservándolo en la zona de carga.
     *
     * @param idPedido         identificador del pedido
     * @param nombreRepartidor nombre del repartidor que se hará cargo
     * @return el pedido asignado
     * @throws IllegalArgumentException si el pedido o el repartidor no existen
     * @throws IllegalStateException    si el pedido ya no está disponible
     */
    public Pedido asignarRepartidor(int idPedido, String nombreRepartidor) {
        Pedido pedido = zonaDeCarga.buscarPedido(idPedido);
        if (pedido == null) {
            throw new IllegalArgumentException("No existe un pedido con el ID " + idPedido + ".");
        }
        if (nombreRepartidor == null || !getNombresRepartidores().contains(nombreRepartidor)) {
            throw new IllegalArgumentException("Debe seleccionar un repartidor válido.");
        }
        zonaDeCarga.asignarRepartidor(pedido, nombreRepartidor);
        return pedido;
    }

    /**
     * Indica si en este momento se está ejecutando un reparto.
     *
     * @return {@code true} si hay entregas en curso
     */
    public boolean isEntregasEnCurso() { return entregasEnCurso; }

    /**
     * Inicia el reparto concurrente: lanza un hilo por repartidor y espera a que
     * terminen de vaciar la zona de carga.
     *
     * @return el resumen del despacho una vez terminadas las entregas
     * @throws InterruptedException  si el hilo que llama es interrumpido mientras espera
     * @throws IllegalStateException si ya hay un reparto en curso
     */
    public String iniciarEntregas() throws InterruptedException {
        synchronized (this) {
            if (entregasEnCurso) {
                throw new IllegalStateException("Ya hay un reparto en curso.");
            }
            entregasEnCurso = true;
        }
        try {
            ExecutorService executor = Executors.newFixedThreadPool(repartidores.size());
            for (Repartidor repartidor : repartidores) {
                executor.submit(repartidor);
            }
            executor.shutdown();
            if (!executor.awaitTermination(ESPERA_MAXIMA_SEGUNDOS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            return getResumen();
        } finally {
            entregasEnCurso = false;
        }
    }

    /**
     * Arma el resumen del despacho con las entregas de cada repartidor y el estado
     * general de la zona de carga.
     *
     * @return el resumen del despacho en texto
     */
    public String getResumen() {
        StringBuilder resumen = new StringBuilder();
        for (Repartidor repartidor : repartidores) {
            resumen.append("- ").append(repartidor.getNombre())
                    .append(" entregó ").append(repartidor.getEntregas())
                    .append(" pedido(s).\n");
        }
        resumen.append("\nEntregas registradas: ").append(zonaDeCarga.getEntregados())
                .append(" de ").append(zonaDeCarga.getTotalPedidos()).append(" pedidos.\n\n");
        resumen.append(zonaDeCarga.todosEntregados()
                ? "Todos los pedidos han sido entregados correctamente."
                : "Quedaron pedidos sin entregar en la zona de carga.");
        return resumen.toString();
    }
}
