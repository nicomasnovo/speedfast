package cl.duocuc.view;

import cl.duocuc.controllers.RepartidorController;
import cl.duocuc.model.Repartidor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;

/**
 * Sección para registrar un repartidor. No existía una ventana para esta
 * operación, pero la lógica sí estaba disponible en el controlador: esta vista
 * solo recoge el nombre y llama a
 * {@link RepartidorController#agregarRepartidor(String)}, que valida, inserta en
 * MySQL y agrega el repartidor a la lista en memoria.
 * <p>
 * La tabla inferior muestra los repartidores que hoy tiene el controlador, para
 * confirmar que el registro quedó guardado.
 */
public class PanelRegistrarRepartidor extends PanelSeccion {

    /** Columnas del listado de repartidores. */
    private static final String[] COLUMNAS = {"ID", "Nombre"};

    private final RepartidorController repartidorController;

    /** Aviso que la ventana principal usa para refrescar las demás secciones. */
    private final Runnable alRegistrar;

    private final JTextField campoNombre = new JTextField(20);
    private final DefaultTableModel modeloTabla;

    /**
     * Crea el formulario de registro de repartidores.
     *
     * @param repartidorController controlador de repartidores compartido
     * @param alRegistrar          acción que se ejecuta cuando se registra un repartidor
     */
    public PanelRegistrarRepartidor(RepartidorController repartidorController,
                                    Runnable alRegistrar) {
        super("Registrar Repartidor");
        this.repartidorController = repartidorController;
        this.alRegistrar = alRegistrar;
        this.modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        add(crearCuerpo(), BorderLayout.CENTER);
        add(crearBotonera(), BorderLayout.SOUTH);

        refrescar();
    }

    /**
     * Arma el formulario y, debajo, la tabla con los repartidores registrados.
     *
     * @return el panel central de la sección
     */
    private JPanel crearCuerpo() {
        JPanel formulario = crearFormulario();
        GridBagConstraints restricciones = crearRestricciones();
        agregarFila(formulario, restricciones, 0, "Nombre:", campoNombre);

        JTable tabla = new JTable(modeloTabla);
        tabla.setFillsViewportHeight(true);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createTitledBorder("Repartidores registrados"));

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(formulario, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
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
        panel.add(botonLimpiar);
        panel.add(botonGuardar);
        return panel;
    }

    /**
     * Entrega el nombre escrito al controlador e informa el resultado.
     */
    private void guardar() {
        try {
            Repartidor repartidor = repartidorController.agregarRepartidor(campoNombre.getText());

            // El controlador devuelve null si el repartidor no se pudo guardar en
            // la base de datos; en ese caso ya avisó del error al usuario.
            if (repartidor == null) {
                return;
            }

            JOptionPane.showMessageDialog(this,
                    "Repartidor registrado correctamente: " + repartidor.getNombre() + ".");
            limpiar();
            refrescar();
            alRegistrar.run();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Deja el formulario en blanco.
     */
    public void limpiar() {
        campoNombre.setText("");
        campoNombre.requestFocusInWindow();
    }

    /**
     * Vuelve a pedir los repartidores al controlador y rearma la tabla.
     */
    public void refrescar() {
        modeloTabla.setRowCount(0);
        for (Repartidor repartidor : repartidorController.getRepartidores()) {
            modeloTabla.addRow(new Object[]{repartidor.getId(), repartidor.getNombre()});
        }
    }
}
