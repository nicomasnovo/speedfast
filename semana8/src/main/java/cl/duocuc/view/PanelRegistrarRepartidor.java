package cl.duocuc.view;

import cl.duocuc.controllers.RepartidorController;
import cl.duocuc.exception.PersistenciaException;
import cl.duocuc.model.Repartidor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.List;

/**
 * Sección con el CRUD de repartidores: registra, edita, elimina y lista. La
 * lógica ya estaba disponible en el controlador, así que esta vista solo recoge
 * el nombre y la fila seleccionada y llama a
 * {@link RepartidorController#agregarRepartidor(String)},
 * {@link RepartidorController#editarRepartidor(int, String)} o
 * {@link RepartidorController#eliminarRepartidor(int)}, que validan y guardan en
 * MySQL a través del DAO.
 * <p>
 * La tabla inferior es de solo lectura y muestra los repartidores que hoy tiene
 * el controlador. Para editar hay que seleccionar una fila, cambiar el nombre y
 * pulsar Editar.
 */
public class PanelRegistrarRepartidor extends PanelSeccion {

    /** Columnas del listado de repartidores. */
    private static final String[] COLUMNAS = {"ID", "Nombre", "Entregas"};

    private final RepartidorController repartidorController;

    /** Aviso que la ventana principal usa para refrescar las demás secciones. */
    private final Runnable alRegistrar;

    private final JTextField campoNombre = new JTextField(20);
    private final DefaultTableModel modeloTabla;
    private final JTable tabla;

    /** Repartidores mostrados, en el mismo orden que las filas de la tabla. */
    private final List<Repartidor> repartidoresMostrados = new ArrayList<>();

    /**
     * Crea el formulario de registro de repartidores.
     *
     * @param repartidorController controlador de repartidores compartido
     * @param alRegistrar          acción que se ejecuta cuando se registra un repartidor
     */
    public PanelRegistrarRepartidor(RepartidorController repartidorController,
                                    Runnable alRegistrar) {
        super("Gestión de Repartidores");
        this.repartidorController = repartidorController;
        this.alRegistrar = alRegistrar;
        this.modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.tabla = new JTable(modeloTabla);
        this.tabla.setFillsViewportHeight(true);
        this.tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Al elegir una fila, su nombre pasa al formulario para poder editarlo.
        this.tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarSeleccionEnFormulario();
            }
        });

        add(crearCuerpo(), BorderLayout.CENTER);

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

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createTitledBorder("Repartidores registrados"));

        // El formulario y los botones del CRUD van sobre la tabla, como en la
        // interfaz referencial de la actividad.
        JPanel encabezado = new JPanel(new BorderLayout(0, 8));
        encabezado.add(formulario, BorderLayout.NORTH);
        encabezado.add(crearBarraDeAcciones(crearBotones()), BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(encabezado, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Crea los botones del CRUD con sus acciones.
     *
     * @return los botones en el orden en que se muestran
     */
    private JButton[] crearBotones() {
        JButton botonNuevo = crearBotonAccion("Nuevo", '+', COLOR_NUEVO);
        JButton botonGuardar = crearBotonAccion("Guardar", '✓', COLOR_NUEVO);
        JButton botonEditar = crearBotonAccion("Editar", '✎', COLOR_EDITAR);
        JButton botonEliminar = crearBotonAccion("Eliminar", '✕', COLOR_ELIMINAR);
        JButton botonActualizar = crearBotonAccion("Actualizar", '↻', COLOR_ACTUALIZAR);

        botonNuevo.addActionListener(e -> limpiar());
        botonGuardar.addActionListener(e -> guardar());
        botonEditar.addActionListener(e -> editar());
        botonEliminar.addActionListener(e -> eliminar());
        botonActualizar.addActionListener(e -> recargarDesdeBaseDeDatos());

        return new JButton[]{
                botonNuevo, botonGuardar, botonEditar, botonEliminar, botonActualizar};
    }

    /**
     * Deja el formulario en blanco y sin fila seleccionada, para registrar un
     * repartidor nuevo sin arrastrar los datos del que estaba seleccionado.
     */
    public void limpiar() {
        tabla.clearSelection();
        campoNombre.setText("");
        campoNombre.requestFocusInWindow();
    }

    /**
     * Entrega el nombre escrito al controlador e informa el resultado.
     */
    private void guardar() {
        try {
            Repartidor repartidor = repartidorController.agregarRepartidor(campoNombre.getText());

            if (repartidor == null) {
                mostrarError("No se pudo registrar el repartidor en la base de datos.");
                return;
            }

            JOptionPane.showMessageDialog(this,
                    "Repartidor registrado correctamente: " + repartidor.getNombre() + ".");
            limpiar();
            refrescar();
            alRegistrar.run();
        } catch (IllegalArgumentException | PersistenciaException ex) {
            mostrarError(ex.getMessage());
        }
    }

    /**
     * Cambia el nombre del repartidor seleccionado por el que hay en el formulario.
     */
    private void editar() {
        Repartidor repartidor = getRepartidorSeleccionado();
        if (repartidor == null) {
            mostrarError("Debe seleccionar un repartidor de la tabla para editarlo.");
            return;
        }

        try {
            if (!repartidorController.editarRepartidor(repartidor.getId(),
                    campoNombre.getText())) {
                mostrarError("No se pudo actualizar el repartidor con ID " + repartidor.getId()
                        + ": puede que ya no exista en la base de datos.");
                recargarDesdeBaseDeDatos();
                return;
            }

            JOptionPane.showMessageDialog(this, "Repartidor actualizado correctamente.");
            refrescar();
            alRegistrar.run();
        } catch (IllegalArgumentException | IllegalStateException | PersistenciaException ex) {
            mostrarError(ex.getMessage());
        }
    }

    /**
     * Elimina el repartidor seleccionado, después de pedir confirmación.
     */
    private void eliminar() {
        Repartidor repartidor = getRepartidorSeleccionado();
        if (repartidor == null) {
            mostrarError("Debe seleccionar un repartidor de la tabla para eliminarlo.");
            return;
        }

        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Eliminar al repartidor " + repartidor.getNombre() + "?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            if (!repartidorController.eliminarRepartidor(repartidor.getId())) {
                mostrarError("No se pudo eliminar el repartidor con ID " + repartidor.getId()
                        + ": puede que ya no exista en la base de datos.");
            } else {
                JOptionPane.showMessageDialog(this, "Repartidor eliminado correctamente.");
            }
            limpiar();
            refrescar();
            alRegistrar.run();
        } catch (IllegalStateException | PersistenciaException ex) {
            // Por ejemplo, cuando el repartidor tiene entregas registradas: se
            // avisa y la aplicación sigue funcionando.
            mostrarError(ex.getMessage());
        }
    }

    /**
     * Pide al controlador que vuelva a leer los repartidores desde MySQL y rearma
     * la tabla. La vista no consulta la base de datos: solo llama al controlador.
     */
    public void recargarDesdeBaseDeDatos() {
        try {
            repartidorController.cargarRepartidoresDesdeDB();
        } catch (PersistenciaException ex) {
            mostrarError(ex.getMessage());
        }
        refrescar();
    }

    /**
     * Vuelve a pedir los repartidores al controlador y rearma la tabla.
     */
    public void refrescar() {
        modeloTabla.setRowCount(0);
        repartidoresMostrados.clear();
        for (Repartidor repartidor : repartidorController.getRepartidores()) {
            repartidoresMostrados.add(repartidor);
            modeloTabla.addRow(new Object[]{
                    repartidor.getId(),
                    repartidor.getNombre(),
                    repartidor.getEntregas()
            });
        }
    }

    /**
     * Copia al formulario el nombre del repartidor seleccionado en la tabla.
     */
    private void mostrarSeleccionEnFormulario() {
        Repartidor repartidor = getRepartidorSeleccionado();
        if (repartidor != null) {
            campoNombre.setText(repartidor.getNombre());
        }
    }

    /**
     * Obtiene el repartidor de la fila seleccionada en la tabla.
     *
     * @return el repartidor seleccionado, o {@code null} si no hay fila elegida
     */
    private Repartidor getRepartidorSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= repartidoresMostrados.size()) {
            return null;
        }
        return repartidoresMostrados.get(fila);
    }

    /**
     * Muestra un mensaje de error al usuario.
     *
     * @param mensaje texto que se muestra
     */
    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
