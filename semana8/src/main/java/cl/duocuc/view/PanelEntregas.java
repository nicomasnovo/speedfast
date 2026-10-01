package cl.duocuc.view;

import cl.duocuc.controllers.EntregaController;
import cl.duocuc.exception.PersistenciaException;
import cl.duocuc.model.Entrega;
import cl.duocuc.model.Pedido;
import cl.duocuc.model.Repartidor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Sección con el CRUD completo de la tabla {@code entrega}: registra, edita,
 * elimina y lista las entregas.
 * <p>
 * Registrar una entrega es dejar constancia de que el pedido llegó a su destino,
 * por lo que el pedido queda entregado a nombre del repartidor de la entrega. No
 * se confunde con asignar un pedido, que solo reserva un pedido pendiente y se
 * hace con el botón Asignar de la sección Pedidos.
 *
 * <p>
 * Es solo presentación. Toma los datos del formulario y llama al
 * {@link EntregaController}, que valida, aplica las reglas de negocio y delega la
 * persistencia en el DAO. La vista no consulta la base de datos ni conoce el DAO
 * (vista → controlador → DAO → ConexionBD → MySQL) y muestra los mensajes con
 * {@link JOptionPane}, incluidos los errores de base de datos que llegan como
 * {@link PersistenciaException}.
 */
public class PanelEntregas extends PanelSeccion {

    /** Columnas del listado de entregas. */
    private static final String[] COLUMNAS = {"ID", "Pedido", "Repartidor", "Fecha", "Hora"};

    /** Formato con que se muestra y se escribe la hora. */
    private static final DateTimeFormatter FORMATO_HORA =
            DateTimeFormatter.ofPattern(EntregaController.FORMATO_HORA);

    private final EntregaController entregaController;

    /** Aviso que la ventana principal usa para refrescar las demás secciones. */
    private final Runnable alCambiar;

    private final DefaultTableModel modeloTabla;
    private final JTable tabla;

    /** Entregas que se están mostrando, en el mismo orden que las filas de la tabla. */
    private final List<Entrega> entregasMostradas = new ArrayList<>();

    /**
     * Crea la sección de gestión de entregas.
     *
     * @param entregaController controlador de entregas compartido
     * @param alCambiar         acción que se ejecuta cuando cambian los datos
     */
    public PanelEntregas(EntregaController entregaController, Runnable alCambiar) {
        super("Gestión de Entregas");
        this.entregaController = entregaController;
        this.alCambiar = alCambiar;

        this.modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.tabla = new JTable(modeloTabla);
        this.tabla.setFillsViewportHeight(true);
        this.tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createTitledBorder("Entregas registradas"));

        JPanel encabezado = new JPanel(new BorderLayout(0, 8));
        encabezado.add(crearBarraDeAcciones(crearBotones()), BorderLayout.NORTH);

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(encabezado, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        add(panel, BorderLayout.CENTER);

        recargar();
    }

    /**
     * Crea los botones del CRUD con sus acciones.
     *
     * @return los botones en el orden en que se muestran
     */
    private JButton[] crearBotones() {
        JButton botonNuevo = crearBotonAccion("Nuevo", '+', COLOR_NUEVO);
        JButton botonEditar = crearBotonAccion("Editar", '✎', COLOR_EDITAR);
        JButton botonEliminar = crearBotonAccion("Eliminar", '✕', COLOR_ELIMINAR);
        JButton botonActualizar = crearBotonAccion("Actualizar", '↻', COLOR_ACTUALIZAR);

        botonNuevo.addActionListener(e -> abrirVentanaNuevaEntrega());
        botonEditar.addActionListener(e -> editar());
        botonEliminar.addActionListener(e -> eliminar());
        botonActualizar.addActionListener(e -> recargar());

        return new JButton[]{botonNuevo, botonEditar, botonEliminar, botonActualizar};
    }

    /**
     * Abre un formulario emergente (JDialog modal) para registrar una entrega.
     */
    private void abrirVentanaNuevaEntrega() {
        List<Pedido> pedidosDisponibles = entregaController.getPedidosDisponibles();
        List<Repartidor> repartidoresDisponibles = entregaController.getRepartidoresDisponibles();

        if (pedidosDisponibles.isEmpty() || repartidoresDisponibles.isEmpty()) {
            mostrarError("Debe haber pedidos y repartidores disponibles para registrar una entrega.");
            return;
        }

        JComboBox<Pedido> comboPedidos = new JComboBox<>(pedidosDisponibles.toArray(new Pedido[0]));
        JComboBox<Repartidor> comboRepartidores = new JComboBox<>(repartidoresDisponibles.toArray(new Repartidor[0]));
        JTextField campoFecha = new JTextField(LocalDate.now().toString(), 12);
        JTextField campoHora = new JTextField(LocalTime.now().format(FORMATO_HORA), 12);

        JPanel formulario = crearFormulario();
        GridBagConstraints restricciones = crearRestricciones();
        agregarFila(formulario, restricciones, 0, "Pedido:", comboPedidos);
        agregarFila(formulario, restricciones, 1, "Repartidor:", comboRepartidores);
        agregarFila(formulario, restricciones, 2, "Fecha (" + EntregaController.FORMATO_FECHA + "):", campoFecha);
        agregarFila(formulario, restricciones, 3, "Hora (" + EntregaController.FORMATO_HORA + "):", campoHora);

        Window ventanaPadre = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(ventanaPadre, "Registrar Nueva Entrega", Dialog.ModalityType.APPLICATION_MODAL);

        JButton botonGuardar = crearBotonAccion("Guardar", '✓', COLOR_NUEVO);
        JButton botonCancelar = crearBotonAccion("Cancelar", '✕', COLOR_ELIMINAR);

        botonGuardar.addActionListener(ev -> {
            Pedido pedido = (Pedido) comboPedidos.getSelectedItem();
            Repartidor repartidor = (Repartidor) comboRepartidores.getSelectedItem();
            if (pedido == null || repartidor == null) {
                mostrarError("Debe seleccionar un pedido y un repartidor.");
                return;
            }

            try {
                Entrega entrega = entregaController.registrarEntrega(
                        pedido.getId(), repartidor.getId(),
                        campoFecha.getText(), campoHora.getText());

                if (entrega == null) {
                    mostrarError("No se pudo registrar la entrega en la base de datos.");
                    return;
                }

                JOptionPane.showMessageDialog(dialog,
                        "Entrega registrada correctamente:\n" + entrega
                                + "\n\nEl pedido #" + pedido.getId()
                                + " queda entregado por " + repartidor.getNombre() + ".");
                dialog.dispose();
                recargar();
                if (alCambiar != null) {
                    alCambiar.run();
                }
            } catch (IllegalArgumentException | IllegalStateException | PersistenciaException ex) {
                mostrarError(ex.getMessage());
            }
        });

        botonCancelar.addActionListener(ev -> dialog.dispose());

        JPanel botonera = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        botonera.add(botonCancelar);
        botonera.add(botonGuardar);

        JPanel contenedor = new JPanel(new BorderLayout(10, 10));
        contenedor.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        contenedor.add(formulario, BorderLayout.CENTER);
        contenedor.add(botonera, BorderLayout.SOUTH);

        dialog.getContentPane().add(contenedor);
        dialog.pack();
        dialog.setLocationRelativeTo(ventanaPadre);
        dialog.setVisible(true);
    }

    /**
     * Abre un formulario emergente para editar la entrega seleccionada.
     */
    private void editar() {
        Entrega entrega = getEntregaSeleccionada();
        if (entrega == null) {
            mostrarError("Debe seleccionar una entrega de la tabla para editarla.");
            return;
        }

        List<Pedido> pedidosDisponibles = entregaController.getPedidosDisponibles();
        List<Repartidor> repartidoresDisponibles = entregaController.getRepartidoresDisponibles();

        JComboBox<Pedido> comboPedidos = new JComboBox<>(pedidosDisponibles.toArray(new Pedido[0]));
        JComboBox<Repartidor> comboRepartidores = new JComboBox<>(repartidoresDisponibles.toArray(new Repartidor[0]));
        JTextField campoFecha = new JTextField(entrega.getFecha().toString(), 12);
        JTextField campoHora = new JTextField(entrega.getHora().format(FORMATO_HORA), 12);

        seleccionarPedidoEnCombo(comboPedidos, entrega.getIdPedido());
        seleccionarRepartidorEnCombo(comboRepartidores, entrega.getIdRepartidor());

        JPanel formulario = crearFormulario();
        GridBagConstraints restricciones = crearRestricciones();
        agregarFila(formulario, restricciones, 0, "Pedido:", comboPedidos);
        agregarFila(formulario, restricciones, 1, "Repartidor:", comboRepartidores);
        agregarFila(formulario, restricciones, 2, "Fecha (" + EntregaController.FORMATO_FECHA + "):", campoFecha);
        agregarFila(formulario, restricciones, 3, "Hora (" + EntregaController.FORMATO_HORA + "):", campoHora);

        int opcion = JOptionPane.showConfirmDialog(this, formulario,
                "Editar entrega #" + entrega.getId(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) {
            return;
        }

        Pedido pedido = (Pedido) comboPedidos.getSelectedItem();
        Repartidor repartidor = (Repartidor) comboRepartidores.getSelectedItem();
        if (pedido == null || repartidor == null) {
            mostrarError("Debe seleccionar el pedido y el repartidor de la entrega.");
            return;
        }

        try {
            boolean editada = entregaController.editarEntrega(
                    entrega.getId(), pedido.getId(), repartidor.getId(),
                    campoFecha.getText(), campoHora.getText());

            if (!editada) {
                mostrarError("No se pudo actualizar la entrega #" + entrega.getId()
                        + ": puede que ya no exista en la base de datos.");
                recargar();
                return;
            }

            JOptionPane.showMessageDialog(this,
                    "Entrega #" + entrega.getId() + " actualizada correctamente.");
            recargar();
            if (alCambiar != null) {
                alCambiar.run();
            }
        } catch (IllegalArgumentException | IllegalStateException | PersistenciaException ex) {
            mostrarError(ex.getMessage());
        }
    }

    /**
     * Elimina la entrega seleccionada, después de pedir confirmación.
     */
    private void eliminar() {
        Entrega entrega = getEntregaSeleccionada();
        if (entrega == null) {
            mostrarError("Debe seleccionar una entrega de la tabla para eliminarla.");
            return;
        }

        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Eliminar la entrega #" + entrega.getId()
                        + " del pedido #" + entrega.getIdPedido() + "?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            if (!entregaController.eliminarEntrega(entrega.getId())) {
                mostrarError("No se pudo eliminar la entrega #" + entrega.getId()
                        + ": puede que ya no exista en la base de datos.");
            } else {
                JOptionPane.showMessageDialog(this,
                        "Entrega #" + entrega.getId() + " eliminada correctamente.");
            }
            recargar();
            if (alCambiar != null) {
                alCambiar.run();
            }
        } catch (PersistenciaException ex) {
            mostrarError(ex.getMessage());
        }
    }

    /**
     * Vuelve a pedir las entregas al controlador y refresca la tabla.
     */
    public void recargar() {
        refrescarTabla();
    }

    /**
     * Reconstruye las filas de la tabla con la lista actualizada desde la BD.
     */
    private void refrescarTabla() {
        modeloTabla.setRowCount(0);
        entregasMostradas.clear();

        List<Entrega> entregas;
        try {
            entregas = entregaController.listarEntregas();
        } catch (PersistenciaException ex) {
            mostrarError(ex.getMessage());
            return;
        }

        for (Entrega entrega : entregas) {
            entregasMostradas.add(entrega);
            modeloTabla.addRow(new Object[]{
                    entrega.getId(),
                    "#" + entrega.getIdPedido()
                            + (entrega.getDireccionPedido() != null
                            ? " - " + entrega.getDireccionPedido() : ""),
                    entrega.getNombreRepartidor() != null
                            ? entrega.getNombreRepartidor()
                            : "ID " + entrega.getIdRepartidor(),
                    entrega.getFecha(),
                    entrega.getHora().format(FORMATO_HORA)
            });
        }
    }

    /**
     * Obtiene la entrega de la fila seleccionada en la tabla.
     *
     * @return la entrega seleccionada, o {@code null} si no hay ninguna fila elegida
     */
    private Entrega getEntregaSeleccionada() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= entregasMostradas.size()) {
            return null;
        }
        return entregasMostradas.get(fila);
    }

    private void seleccionarPedidoEnCombo(JComboBox<Pedido> combo, int idPedido) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).getId() == idPedido) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void seleccionarRepartidorEnCombo(JComboBox<Repartidor> combo, int idRepartidor) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).getId() == idRepartidor) {
                combo.setSelectedIndex(i);
                return;
            }
        }
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