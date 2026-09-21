package cl.duocuc.view;

import cl.duocuc.controllers.PedidoController;
import cl.duocuc.controllers.RepartidorController;
import cl.duocuc.model.Pedido;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

/**
 * Ventana para asignar un repartidor a un pedido pendiente. Reemplaza la
 * asignación que antes quedaba implícita en la consola: la vista solo recoge la
 * selección y llama al {@link RepartidorController}, que reserva el pedido en la
 * zona de carga para ese repartidor.
 */
public class VentanaAsignarRepartidor extends JFrame {

    private final PedidoController pedidoController;
    private final RepartidorController repartidorController;

    /** Aviso que la ventana principal usa para refrescar las demás vistas. */
    private final Runnable alAsignar;

    private final JComboBox<Pedido> comboPedidos = new JComboBox<>();
    private final JComboBox<String> comboRepartidores = new JComboBox<>();
    private final JButton botonAsignar = new JButton("Asignar");

    /**
     * Crea la ventana de asignación.
     *
     * @param pedidoController     controlador de pedidos compartido
     * @param repartidorController controlador de repartidores compartido
     * @param alAsignar            acción que se ejecuta cuando se asigna un pedido
     */
    public VentanaAsignarRepartidor(PedidoController pedidoController,
                                    RepartidorController repartidorController,
                                    Runnable alAsignar) {
        this.pedidoController = pedidoController;
        this.repartidorController = repartidorController;
        this.alAsignar = alAsignar;

        setTitle("SpeedFast - Asignar Repartidor");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        comboRepartidores.setModel(new DefaultComboBoxModel<>(
                repartidorController.getNombresRepartidores().toArray(new String[0])));

        add(crearFormulario(), BorderLayout.CENTER);
        add(crearBotonera(), BorderLayout.SOUTH);

        recargarPendientes();
        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Arma el formulario con los dos desplegables de selección.
     *
     * @return el panel del formulario
     */
    private JPanel crearFormulario() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 5, 15));

        GridBagConstraints restricciones = new GridBagConstraints();
        restricciones.insets = new Insets(5, 5, 5, 5);
        restricciones.anchor = GridBagConstraints.LINE_START;

        restricciones.gridx = 0;
        restricciones.gridy = 0;
        panel.add(new JLabel("Pedido pendiente:"), restricciones);

        restricciones.gridx = 1;
        restricciones.fill = GridBagConstraints.HORIZONTAL;
        restricciones.weightx = 1;
        panel.add(comboPedidos, restricciones);

        restricciones.gridx = 0;
        restricciones.gridy = 1;
        restricciones.fill = GridBagConstraints.NONE;
        restricciones.weightx = 0;
        panel.add(new JLabel("Repartidor:"), restricciones);

        restricciones.gridx = 1;
        restricciones.fill = GridBagConstraints.HORIZONTAL;
        restricciones.weightx = 1;
        panel.add(comboRepartidores, restricciones);
        return panel;
    }

    /**
     * Arma los botones Actualizar y Asignar.
     *
     * @return el panel de botones
     */
    private JPanel crearBotonera() {
        JButton botonActualizar = new JButton("Actualizar");
        botonActualizar.addActionListener(e -> recargarPendientes());
        botonAsignar.addActionListener(e -> asignar());

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 15, 10, 15));
        panel.add(botonActualizar);
        panel.add(botonAsignar);
        return panel;
    }

    /**
     * Vuelve a pedir al controlador los pedidos pendientes y rearma el desplegable.
     */
    public void recargarPendientes() {
        List<Pedido> pendientes = pedidoController.getPedidosPendientes();
        comboPedidos.setModel(new DefaultComboBoxModel<>(pendientes.toArray(new Pedido[0])));
        botonAsignar.setEnabled(!pendientes.isEmpty());
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
            JOptionPane.showMessageDialog(this, "Pedido #" + pedido.getId()
                    + " asignado a " + repartidor + ".");
            recargarPendientes();
            alAsignar.run();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            recargarPendientes();
        }
    }
}
