package cl.duocuc.view;

import cl.duocuc.controllers.PedidoController;
import cl.duocuc.model.Pedido;
import cl.duocuc.model.TipoPedido;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Formulario para registrar un pedido. Reemplaza el ingreso de pedidos que antes
 * se hacía por consola: toma los datos, los entrega al {@link PedidoController} y
 * muestra el resultado con un {@link JOptionPane}.
 */
public class VentanaRegistroPedido extends JFrame {

    private final PedidoController pedidoController;

    /** Aviso que la ventana principal usa para refrescar las demás vistas. */
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
    public VentanaRegistroPedido(PedidoController pedidoController, Runnable alRegistrar) {
        this.pedidoController = pedidoController;
        this.alRegistrar = alRegistrar;
        this.comboTipo = new JComboBox<>(pedidoController.getTiposDePedido());

        setTitle("SpeedFast - Registrar Pedido");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        add(crearFormulario(), BorderLayout.CENTER);
        add(crearBotonera(), BorderLayout.SOUTH);

        limpiar();
        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Arma el formulario con las etiquetas y campos del pedido.
     *
     * @return el panel del formulario
     */
    private JPanel crearFormulario() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 5, 15));

        GridBagConstraints restricciones = new GridBagConstraints();
        restricciones.insets = new Insets(5, 5, 5, 5);
        restricciones.anchor = GridBagConstraints.LINE_START;

        agregarFila(panel, restricciones, 0, "ID del Pedido:", campoId);
        agregarFila(panel, restricciones, 1, "Dirección:", campoDireccion);
        agregarFila(panel, restricciones, 2, "Tipo de Pedido:", comboTipo);
        return panel;
    }

    /**
     * Agrega una fila etiqueta + campo al formulario.
     *
     * @param panel         panel del formulario
     * @param restricciones restricciones reutilizadas del {@link GridBagLayout}
     * @param fila          número de fila
     * @param texto         texto de la etiqueta
     * @param campo         componente de entrada
     */
    private void agregarFila(JPanel panel, GridBagConstraints restricciones, int fila,
                             String texto, java.awt.Component campo) {
        restricciones.gridx = 0;
        restricciones.gridy = fila;
        restricciones.fill = GridBagConstraints.NONE;
        restricciones.weightx = 0;
        panel.add(new JLabel(texto), restricciones);

        restricciones.gridx = 1;
        restricciones.fill = GridBagConstraints.HORIZONTAL;
        restricciones.weightx = 1;
        panel.add(campo, restricciones);
    }

    /**
     * Arma los botones Guardar y Limpiar.
     *
     * @return el panel de botones
     */
    private JPanel crearBotonera() {
        JButton botonGuardar = new JButton("Guardar");
        JButton botonLimpiar = new JButton("Limpiar");

        botonGuardar.addActionListener(e -> guardar());
        botonLimpiar.addActionListener(e -> limpiar());

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 15, 10, 15));
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

            // El controlador devuelve null si el pedido no se pudo guardar en la
            // base de datos; en ese caso ya avisó del error al usuario.
            if (pedido == null) {
                return;
            }

            JOptionPane.showMessageDialog(this,
                    "Pedido registrado correctamente:\n" + pedido);
            limpiar();
            alRegistrar.run();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Deja el formulario en blanco y propone el siguiente identificador libre.
     */
    private void limpiar() {
        campoId.setText(String.valueOf(pedidoController.sugerirId()));
        campoDireccion.setText("");
        comboTipo.setSelectedIndex(0);
        campoDireccion.requestFocusInWindow();
    }
}
