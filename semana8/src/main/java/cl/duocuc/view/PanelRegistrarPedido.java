package cl.duocuc.view;

import cl.duocuc.controllers.PedidoController;
import cl.duocuc.exception.PersistenciaException;
import cl.duocuc.model.Pedido;
import cl.duocuc.model.TipoPedido;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;

/**
 * Formulario para registrar un pedido. Es un {@link JPanel}, así que no abre una
 * ventana por su cuenta: lo muestra el botón Nuevo de {@link PanelPedidos} dentro
 * de un diálogo modal.
 * <p>
 * La lógica no cambia: toma los datos, los entrega al {@link PedidoController} y
 * muestra el resultado con un {@link JOptionPane}.
 */
public class PanelRegistrarPedido extends PanelSeccion {

    private final PedidoController pedidoController;

    /** Aviso que la ventana principal usa para refrescar las demás secciones. */
    private final Runnable alRegistrar;

    private final JTextField campoId = new JTextField(10);
    private final JTextField campoDireccion = new JTextField(20);
    private final JComboBox<TipoPedido> comboTipo;

    /**
     * Crea el formulario de registro.
     *
     * @param pedidoController controlador de pedidos compartido
     * @param alRegistrar      acción que se ejecuta cuando se registra un pedido
     */
    public PanelRegistrarPedido(PedidoController pedidoController, Runnable alRegistrar) {
        super("Registrar Pedido");
        this.pedidoController = pedidoController;
        this.alRegistrar = alRegistrar;
        this.comboTipo = new JComboBox<>(pedidoController.getTiposDePedido());

        agregarContenidoSuperior(crearCampos());
        add(crearBotonera(), BorderLayout.SOUTH);

        limpiar();
    }

    /**
     * Arma el formulario con las etiquetas y campos del pedido.
     *
     * @return el panel del formulario
     */
    private JPanel crearCampos() {
        JPanel panel = crearFormulario();
        GridBagConstraints restricciones = crearRestricciones();

        agregarFila(panel, restricciones, 0, "ID del Pedido:", campoId);
        agregarFila(panel, restricciones, 1, "Dirección:", campoDireccion);
        agregarFila(panel, restricciones, 2, "Tipo de Pedido:", comboTipo);
        return panel;
    }

    /**
     * Arma los botones Guardar y Limpiar.
     *
     * @return el panel de botones
     */
    private JPanel crearBotonera() {
        JButton botonGuardar = crearBotonAccion("Guardar", '✓', COLOR_NUEVO);
        JButton botonLimpiar = crearBotonAccion("Limpiar", '↻', COLOR_ACTUALIZAR);

        botonGuardar.addActionListener(e -> guardar());
        botonLimpiar.addActionListener(e -> limpiar());

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        panel.add(botonLimpiar);
        panel.add(botonGuardar);
        return panel;
    }

    /**
     * Entrega los datos del formulario al controlador e informa el resultado.
     */
    private void guardar() {
        try {
            Pedido pedido = pedidoController.registrarPedido(
                    campoId.getText(),
                    campoDireccion.getText(),
                    (TipoPedido) comboTipo.getSelectedItem());

            // El controlador devuelve null si la inserción no afectó ninguna fila.
            if (pedido == null) {
                JOptionPane.showMessageDialog(this,
                        "No se pudo registrar el pedido en la base de datos.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            JOptionPane.showMessageDialog(this,
                    "Pedido registrado correctamente:\n" + pedido);
            limpiar();
            alRegistrar.run();
        } catch (IllegalArgumentException | PersistenciaException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Deja el formulario en blanco y propone el siguiente identificador libre.
     */
    public void limpiar() {
        campoId.setText(String.valueOf(pedidoController.sugerirId()));
        campoDireccion.setText("");
        comboTipo.setSelectedIndex(0);
        campoDireccion.requestFocusInWindow();
    }
}
