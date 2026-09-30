package cl.duocuc.view;

import cl.duocuc.controllers.EntregaController;
import cl.duocuc.exception.PersistenciaException;
import cl.duocuc.model.Entrega;
import cl.duocuc.model.Pedido;
import cl.duocuc.model.Repartidor;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Sección con el CRUD completo de la tabla {@code entrega}: registra, edita,
 * elimina y lista las entregas.
 * <p>
 * Es solo presentación. Toma los datos del formulario y llama al
 * {@link EntregaController}, que valida, aplica las reglas de negocio y delega la
 * persistencia en el DAO. La vista no consulta la base de datos ni conoce el DAO
 * (vista → controlador → DAO → ConexionBD → MySQL) y muestra los mensajes con
 * {@link JOptionPane}, incluidos los errores de base de datos que llegan como
 * {@link PersistenciaException}.
 * <p>
 * El pedido y el repartidor se eligen en desplegables, para no escribir
 * identificadores a mano, y la tabla es de solo lectura: para modificar una
 * entrega hay que seleccionar su fila, cambiar el formulario y pulsar Editar.
 */
public class PanelVerEntregas extends PanelSeccion {

    /** Columnas del listado de entregas. */
    private static final String[] COLUMNAS = {"ID", "Pedido", "Repartidor", "Fecha", "Hora"};

    /** Formato con que se muestra y se escribe la hora. */
    private static final DateTimeFormatter FORMATO_HORA =
            DateTimeFormatter.ofPattern(EntregaController.FORMATO_HORA);

    private final EntregaController entregaController;

    /** Aviso que la ventana principal usa para refrescar las demás secciones. */
    private final Runnable alCambiar;

    private final JComboBox<Pedido> comboPedidos = new JComboBox<>();
    private final JComboBox<Repartidor> comboRepartidores = new JComboBox<>();
    private final JTextField campoFecha = new JTextField(12);
    private final JTextField campoHora = new JTextField(12);

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
    public PanelVerEntregas(EntregaController entregaController, Runnable alCambiar) {
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
        // Al elegir una fila, sus datos pasan al formulario para poder editarla.
        this.tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarSeleccionEnFormulario();
            }
        });

        add(crearCuerpo(), BorderLayout.CENTER);

        recargar();
        limpiar();
    }

    /**
     * Arma el formulario de la entrega y, debajo, la tabla con las entregas
     * registradas.
     *
     * @return el panel central de la sección
     */
    private JPanel crearCuerpo() {
        JPanel formulario = crearFormulario();
        GridBagConstraints restricciones = crearRestricciones();
        agregarFila(formulario, restricciones, 0, "Pedido:", comboPedidos);
        agregarFila(formulario, restricciones, 1, "Repartidor:", comboRepartidores);
        agregarFila(formulario, restricciones, 2,
                "Fecha (" + EntregaController.FORMATO_FECHA + "):", campoFecha);
        agregarFila(formulario, restricciones, 3,
                "Hora (" + EntregaController.FORMATO_HORA + "):", campoHora);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createTitledBorder("Entregas registradas"));

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
        botonActualizar.addActionListener(e -> recargar());

        return new JButton[]{
                botonNuevo, botonGuardar, botonEditar, botonEliminar, botonActualizar};
    }

    /**
     * Vuelve a pedir al controlador los pedidos, los repartidores y las entregas, y
     * rearma los desplegables y la tabla con lo que hay en la base de datos.
     */
    public void recargar() {
        Pedido pedidoElegido = (Pedido) comboPedidos.getSelectedItem();
        Repartidor repartidorElegido = (Repartidor) comboRepartidores.getSelectedItem();

        comboPedidos.setModel(new DefaultComboBoxModel<>(
                entregaController.getPedidosDisponibles().toArray(new Pedido[0])));
        comboRepartidores.setModel(new DefaultComboBoxModel<>(
                entregaController.getRepartidoresDisponibles().toArray(new Repartidor[0])));

        // Se intenta mantener la selección que tenía el usuario antes de recargar.
        if (pedidoElegido != null) {
            seleccionarPedido(pedidoElegido.getId());
        }
        if (repartidorElegido != null) {
            seleccionarRepartidor(repartidorElegido.getId());
        }

        refrescarTabla();
    }

    /**
     * Vuelve a leer las entregas y reconstruye las filas de la tabla.
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
     * Deja el formulario listo para una entrega nueva: sin fila seleccionada y con
     * la fecha y la hora actuales propuestas.
     */
    public void limpiar() {
        tabla.clearSelection();
        campoFecha.setText(LocalDate.now().toString());
        campoHora.setText(LocalTime.now().format(FORMATO_HORA));
    }

    /**
     * Copia al formulario los datos de la entrega seleccionada en la tabla.
     */
    private void mostrarSeleccionEnFormulario() {
        Entrega entrega = getEntregaSeleccionada();
        if (entrega == null) {
            return;
        }
        seleccionarPedido(entrega.getIdPedido());
        seleccionarRepartidor(entrega.getIdRepartidor());
        campoFecha.setText(entrega.getFecha().toString());
        campoHora.setText(entrega.getHora().format(FORMATO_HORA));
    }

    /**
     * Registra una entrega nueva con los datos del formulario.
     */
    private void guardar() {
        Pedido pedido = (Pedido) comboPedidos.getSelectedItem();
        Repartidor repartidor = (Repartidor) comboRepartidores.getSelectedItem();
        if (pedido == null || repartidor == null) {
            mostrarError("Debe haber pedidos y repartidores registrados "
                    + "para poder registrar una entrega.");
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

            JOptionPane.showMessageDialog(this,
                    "Entrega registrada correctamente:\n" + entrega);
            limpiar();
            recargar();
            alCambiar.run();
        } catch (IllegalArgumentException | IllegalStateException | PersistenciaException ex) {
            mostrarError(ex.getMessage());
        }
    }

    /**
     * Guarda en la entrega seleccionada los datos que haya en el formulario.
     */
    private void editar() {
        Entrega entrega = getEntregaSeleccionada();
        if (entrega == null) {
            mostrarError("Debe seleccionar una entrega de la tabla para editarla.");
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
            alCambiar.run();
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
            limpiar();
            recargar();
            alCambiar.run();
        } catch (PersistenciaException ex) {
            mostrarError(ex.getMessage());
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

    /**
     * Deja seleccionado en el desplegable el pedido con el identificador indicado.
     *
     * @param idPedido identificador del pedido que se quiere seleccionar
     */
    private void seleccionarPedido(int idPedido) {
        for (int i = 0; i < comboPedidos.getItemCount(); i++) {
            if (comboPedidos.getItemAt(i).getId() == idPedido) {
                comboPedidos.setSelectedIndex(i);
                return;
            }
        }
    }

    /**
     * Deja seleccionado en el desplegable el repartidor con el identificador
     * indicado.
     *
     * @param idRepartidor identificador del repartidor que se quiere seleccionar
     */
    private void seleccionarRepartidor(int idRepartidor) {
        for (int i = 0; i < comboRepartidores.getItemCount(); i++) {
            if (comboRepartidores.getItemAt(i).getId() == idRepartidor) {
                comboRepartidores.setSelectedIndex(i);
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
