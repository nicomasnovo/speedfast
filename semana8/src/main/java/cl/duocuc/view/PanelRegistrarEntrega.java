package cl.duocuc.view;

import cl.duocuc.controllers.PedidoController;
import cl.duocuc.controllers.RepartidorController;
import cl.duocuc.exception.PersistenciaException;
import cl.duocuc.model.Pedido;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Sección para registrar las entregas. Reúne las dos operaciones que ya existían
 * en la aplicación:
 * <ul>
 *   <li>asignar un pedido pendiente a un repartidor (lo que antes hacía la
 *       ventana {@code VentanaAsignarRepartidor}), y</li>
 *   <li>iniciar el reparto concurrente (el botón "Iniciar Entregas" que antes
 *       estaba en el menú de la {@link VentanaPrincipal}).</li>
 * </ul>
 * La lógica no cambia: la vista solo recoge la selección y llama al
 * {@link RepartidorController}, que reserva el pedido en la zona de carga y
 * ejecuta a los repartidores en sus propios hilos.
 */
public class PanelRegistrarEntrega extends PanelSeccion {

    private final PedidoController pedidoController;
    private final RepartidorController repartidorController;

    /** Aviso que la ventana principal usa para refrescar las demás secciones. */
    private final Runnable alRegistrar;

    private final JComboBox<Pedido> comboPedidos = new JComboBox<>();
    private final JComboBox<String> comboRepartidores = new JComboBox<>();
    private final JButton botonAsignar = crearBotonAccion("Asignar", '✓', COLOR_NUEVO);
    private final JButton botonIniciarEntregas =
            crearBotonAccion("Iniciar Entregas", '▶', COLOR_EDITAR);
    private final JLabel etiquetaEstado = new JLabel(" ");

    /**
     * Crea la sección de entregas.
     *
     * @param pedidoController     controlador de pedidos compartido
     * @param repartidorController controlador de repartidores compartido
     * @param alRegistrar          acción que se ejecuta cuando cambian los datos
     */
    public PanelRegistrarEntrega(PedidoController pedidoController,
                                 RepartidorController repartidorController,
                                 Runnable alRegistrar) {
        super("Registrar Entrega");
        this.pedidoController = pedidoController;
        this.repartidorController = repartidorController;
        this.alRegistrar = alRegistrar;

        agregarContenidoSuperior(crearCampos());
        add(crearBotonera(), BorderLayout.SOUTH);

        recargar();
    }

    /**
     * Arma el formulario con los dos desplegables de selección.
     *
     * @return el panel del formulario
     */
    private JPanel crearCampos() {
        JPanel panel = crearFormulario();
        GridBagConstraints restricciones = crearRestricciones();

        agregarFila(panel, restricciones, 0, "Pedido pendiente:", comboPedidos);
        agregarFila(panel, restricciones, 1, "Repartidor:", comboRepartidores);
        return panel;
    }

    /**
     * Arma la botonera: arriba la asignación del pedido y abajo el inicio del
     * reparto con su mensaje de avance.
     *
     * @return el panel inferior de la sección
     */
    private JPanel crearBotonera() {
        JButton botonActualizar = crearBotonAccion("Actualizar", '↻', COLOR_ACTUALIZAR);
        botonActualizar.addActionListener(e -> recargar());
        botonAsignar.addActionListener(e -> asignar());
        botonIniciarEntregas.addActionListener(e -> iniciarEntregas());

        JPanel asignacion = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        asignacion.add(botonActualizar);
        asignacion.add(botonAsignar);

        JPanel reparto = new JPanel(new BorderLayout(10, 0));
        reparto.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
        reparto.add(etiquetaEstado, BorderLayout.CENTER);
        JPanel contenedorBoton = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        contenedorBoton.add(botonIniciarEntregas);
        reparto.add(contenedorBoton, BorderLayout.EAST);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(asignacion);
        panel.add(Box.createVerticalStrut(5));
        panel.add(new JSeparator());
        panel.add(reparto);
        return panel;
    }

    /**
     * Vuelve a pedir a los controladores los pedidos pendientes y los
     * repartidores disponibles, y rearma los desplegables.
     */
    public void recargar() {
        List<Pedido> pendientes = pedidoController.getPedidosPendientes();
        comboPedidos.setModel(new DefaultComboBoxModel<>(pendientes.toArray(new Pedido[0])));
        botonAsignar.setEnabled(!pendientes.isEmpty());

        comboRepartidores.setModel(new DefaultComboBoxModel<>(
                repartidorController.getNombresRepartidores().toArray(new String[0])));
    }

    /**
     * Entrega la selección al controlador e informa el resultado.
     */
    private void asignar() {
        Pedido pedido = (Pedido) comboPedidos.getSelectedItem();
        if (pedido == null) {
            JOptionPane.showMessageDialog(this, "No hay pedidos pendientes por asignar.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String repartidor = (String) comboRepartidores.getSelectedItem();
        try {
            repartidorController.asignarRepartidor(pedido.getId(), repartidor);

            // La asignación queda hecha en la zona de carga (en memoria); se guarda
            // en MySQL para que no se pierda al recargar los pedidos.
            if (!pedidoController.editarPedido(pedido)) {
                JOptionPane.showMessageDialog(this,
                        "No se pudo guardar la asignación del pedido #" + pedido.getId()
                                + ": puede que ya no exista en la base de datos.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                recargar();
                alRegistrar.run();
                return;
            }

            JOptionPane.showMessageDialog(this, "Pedido #" + pedido.getId()
                    + " asignado a " + repartidor + ".");
            recargar();
            alRegistrar.run();
        } catch (IllegalArgumentException | IllegalStateException | PersistenciaException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            recargar();
        }
    }

    /**
     * Lanza el reparto concurrente sin bloquear el Event Dispatch Thread: la
     * lógica existente del controlador se ejecuta en un {@link SwingWorker} y el
     * resultado se muestra cuando termina.
     */
    private void iniciarEntregas() {
        botonIniciarEntregas.setEnabled(false);
        etiquetaEstado.setText("Reparto en curso...");

        new SwingWorker<String, Void>() {

            @Override
            protected String doInBackground() throws Exception {
                return repartidorController.iniciarEntregas();
            }

            @Override
            protected void done() {
                botonIniciarEntregas.setEnabled(true);
                etiquetaEstado.setText(" ");
                try {
                    String resumen = get();
                    // Los hilos de reparto cambiaron el estado de los pedidos en
                    // memoria; se guardan en MySQL para que el avance persista.
                    pedidoController.guardarEstados();
                    recargar();
                    alRegistrar.run();
                    JOptionPane.showMessageDialog(PanelRegistrarEntrega.this, resumen,
                            "Resumen del despacho", JOptionPane.INFORMATION_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (PersistenciaException e) {
                    // El reparto terminó, pero MySQL no aceptó guardar los estados.
                    JOptionPane.showMessageDialog(PanelRegistrarEntrega.this, e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause() != null ? e.getCause() : e;
                    JOptionPane.showMessageDialog(PanelRegistrarEntrega.this, causa.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
