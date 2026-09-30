package cl.duocuc.view;

import cl.duocuc.controllers.PedidoController;
import cl.duocuc.exception.PersistenciaException;
import cl.duocuc.model.EstadoPedido;
import cl.duocuc.model.Pedido;
import cl.duocuc.model.TipoPedido;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.List;

/**
 * Sección con el listado de pedidos en una {@link JTable} de solo lectura y las
 * operaciones que se hacen sobre él: editar, eliminar, filtrar y volver a leer
 * desde MySQL.
 * <p>
 * Los datos se toman siempre desde el {@link PedidoController}, sin mantener una
 * copia propia y sin consultar la base de datos desde la vista
 * (vista → controlador → DAO → ConexionBD → MySQL). Los mensajes, incluidos los
 * errores de base de datos que llegan como {@link PersistenciaException}, se
 * muestran aquí con {@link JOptionPane}.
 * <p>
 * Los filtros por tipo y por estado solo cambian qué pedidos se muestran: no
 * modifican ningún registro, porque trabajan sobre la lista que entrega el
 * controlador.
 */
public class PanelPedidos extends PanelSeccion {

    /** Columnas del listado. */
    private static final String[] COLUMNAS = {"ID", "Dirección", "Tipo", "Estado", "Repartidor"};

    /** Primera opción de los dos filtros: no filtrar. */
    private static final String TODOS = "Todos";

    private final PedidoController pedidoController;

    /** Acción con que la ventana principal abre la sección de registro. */
    private final Runnable alPedirNuevo;

    private final DefaultTableModel modeloTabla;
    private final JTable tabla;
    private final JLabel etiquetaResumen = new JLabel(" ");

    /**
     * Filtros de tipo y estado. Guardan la opción {@value #TODOS} junto con los
     * valores de los enums reales del proyecto, por lo que su tipo es
     * {@link Object}.
     */
    private final JComboBox<Object> comboFiltroTipo = new JComboBox<>();
    private final JComboBox<Object> comboFiltroEstado = new JComboBox<>();

    /** Pedidos que se están mostrando, en el mismo orden que las filas de la tabla. */
    private final List<Pedido> pedidosMostrados = new ArrayList<>();

    /**
     * Crea el listado de pedidos.
     *
     * @param pedidoController controlador de pedidos compartido
     * @param alPedirNuevo     acción que abre la sección Registrar Pedido
     */
    public PanelPedidos(PedidoController pedidoController, Runnable alPedirNuevo) {
        super("Gestión de Pedidos");
        this.pedidoController = pedidoController;
        this.alPedirNuevo = alPedirNuevo;
        this.modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.tabla = new JTable(modeloTabla);
        this.tabla.setFillsViewportHeight(true);
        this.tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        llenarFiltros();

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createEtchedBorder());

        // Los botones del CRUD y los filtros van sobre la tabla, como en la
        // interfaz referencial de la actividad.
        JPanel encabezado = new JPanel(new BorderLayout(0, 8));
        encabezado.add(crearBarraDeAcciones(crearBotones()), BorderLayout.NORTH);
        encabezado.add(crearFiltros(), BorderLayout.CENTER);

        JPanel cuerpo = new JPanel(new BorderLayout(0, 10));
        cuerpo.add(encabezado, BorderLayout.NORTH);
        cuerpo.add(scroll, BorderLayout.CENTER);

        add(cuerpo, BorderLayout.CENTER);
        add(crearPiePagina(), BorderLayout.SOUTH);

        refrescar();
    }

    /**
     * Carga en los dos filtros la opción {@value #TODOS} y los valores de los
     * enums del proyecto, que entrega el controlador.
     */
    private void llenarFiltros() {
        comboFiltroTipo.addItem(TODOS);
        for (TipoPedido tipo : pedidoController.getTiposDePedido()) {
            comboFiltroTipo.addItem(tipo);
        }
        comboFiltroEstado.addItem(TODOS);
        for (EstadoPedido estado : pedidoController.getEstadosDePedido()) {
            comboFiltroEstado.addItem(estado);
        }

        comboFiltroTipo.addActionListener(e -> refrescar());
        comboFiltroEstado.addActionListener(e -> refrescar());
    }

    /**
     * Arma la fila de filtros que va sobre la tabla.
     *
     * @return el panel de filtros
     */
    private JPanel crearFiltros() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panel.setBorder(BorderFactory.createTitledBorder("Filtros"));
        panel.add(new JLabel("Tipo:"));
        panel.add(comboFiltroTipo);
        panel.add(new JLabel("Estado:"));
        panel.add(comboFiltroEstado);
        return panel;
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

        botonNuevo.addActionListener(e -> alPedirNuevo.run());
        botonEditar.addActionListener(e -> editar());
        botonEliminar.addActionListener(e -> eliminar());
        botonActualizar.addActionListener(e -> recargarDesdeBaseDeDatos());

        return new JButton[]{botonNuevo, botonEditar, botonEliminar, botonActualizar};
    }

    /**
     * Arma el pie con el resumen de lo que se está mostrando.
     *
     * @return el panel del pie de página
     */
    private JPanel crearPiePagina() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(etiquetaResumen, BorderLayout.WEST);
        return panel;
    }

    /**
     * Pide al controlador que vuelva a leer los pedidos desde MySQL y rearma la
     * tabla con lo que haya en la base de datos. La vista no consulta la base de
     * datos: solo llama al controlador.
     */
    public void recargarDesdeBaseDeDatos() {
        try {
            pedidoController.cargarPedidosDesdeDB();
        } catch (PersistenciaException ex) {
            mostrarError(ex.getMessage());
        }
        refrescar();
    }

    /**
     * Vuelve a consultar los pedidos al controlador, aplicando los filtros
     * elegidos, y reconstruye las filas.
     */
    public void refrescar() {
        modeloTabla.setRowCount(0);
        pedidosMostrados.clear();

        for (Pedido pedido : pedidoController.getPedidosFiltrados(
                getFiltroTipo(), getFiltroEstado())) {
            pedidosMostrados.add(pedido);
            String repartidor = pedido.getRepartidorAsignado();
            modeloTabla.addRow(new Object[]{
                    pedido.getId(),
                    pedido.getDireccionEntrega(),
                    pedido.getTipo(),
                    pedido.getEstado(),
                    repartidor != null ? repartidor : "-"
            });
        }
        etiquetaResumen.setText("Mostrando " + pedidosMostrados.size()
                + " de " + pedidoController.getTotalPedidos() + " pedidos. Entregados: "
                + pedidoController.getEntregados() + ".");
    }

    /**
     * Obtiene el tipo elegido en el filtro.
     *
     * @return el tipo por el que se filtra, o {@code null} si está en {@value #TODOS}
     */
    private TipoPedido getFiltroTipo() {
        return comboFiltroTipo.getSelectedItem() instanceof TipoPedido tipo ? tipo : null;
    }

    /**
     * Obtiene el estado elegido en el filtro.
     *
     * @return el estado por el que se filtra, o {@code null} si está en {@value #TODOS}
     */
    private EstadoPedido getFiltroEstado() {
        return comboFiltroEstado.getSelectedItem() instanceof EstadoPedido estado ? estado : null;
    }

    /**
     * Abre un formulario con los datos del pedido seleccionado y entrega los
     * cambios al controlador.
     */
    private void editar() {
        Pedido pedido = getPedidoSeleccionado();
        if (pedido == null) {
            mostrarError("Debe seleccionar un pedido de la tabla para editarlo.");
            return;
        }

        JTextField campoDireccion = new JTextField(pedido.getDireccionEntrega(), 20);
        JComboBox<TipoPedido> comboTipo =
                new JComboBox<>(pedidoController.getTiposDePedido());
        comboTipo.setSelectedItem(pedido.getTipo());
        JComboBox<EstadoPedido> comboEstado =
                new JComboBox<>(pedidoController.getEstadosDePedido());
        comboEstado.setSelectedItem(pedido.getEstado());

        JPanel formulario = crearFormulario();
        GridBagConstraints restricciones = crearRestricciones();
        agregarFila(formulario, restricciones, 0, "Dirección:", campoDireccion);
        agregarFila(formulario, restricciones, 1, "Tipo:", comboTipo);
        agregarFila(formulario, restricciones, 2, "Estado:", comboEstado);

        int opcion = JOptionPane.showConfirmDialog(this, formulario,
                "Editar pedido #" + pedido.getId(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            boolean editado = pedidoController.editarPedido(
                    pedido.getId(),
                    campoDireccion.getText(),
                    (TipoPedido) comboTipo.getSelectedItem(),
                    (EstadoPedido) comboEstado.getSelectedItem());

            if (!editado) {
                mostrarError("No se pudo actualizar el pedido #" + pedido.getId()
                        + ": puede que ya no exista en la base de datos.");
                refrescar();
                return;
            }

            JOptionPane.showMessageDialog(this,
                    "Pedido #" + pedido.getId() + " actualizado correctamente.");
            refrescar();
        } catch (IllegalArgumentException | IllegalStateException | PersistenciaException ex) {
            mostrarError(ex.getMessage());
        }
    }

    /**
     * Elimina el pedido seleccionado, después de pedir confirmación.
     */
    private void eliminar() {
        Pedido pedido = getPedidoSeleccionado();
        if (pedido == null) {
            mostrarError("Debe seleccionar un pedido de la tabla para eliminarlo.");
            return;
        }

        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Eliminar el pedido #" + pedido.getId() + "?\n"
                        + pedido.getDireccionEntrega(),
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            if (!pedidoController.eliminarPedido(pedido.getId())) {
                mostrarError("No se pudo eliminar el pedido #" + pedido.getId()
                        + ": puede que ya no exista en la base de datos.");
            } else {
                JOptionPane.showMessageDialog(this,
                        "Pedido #" + pedido.getId() + " eliminado correctamente.");
            }
            refrescar();
        } catch (IllegalStateException | PersistenciaException ex) {
            // Por ejemplo, cuando el pedido tiene entregas registradas: se avisa y
            // la aplicación sigue funcionando.
            mostrarError(ex.getMessage());
        }
    }

    /**
     * Obtiene el pedido de la fila seleccionada en la tabla.
     *
     * @return el pedido seleccionado, o {@code null} si no hay ninguna fila elegida
     */
    private Pedido getPedidoSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= pedidosMostrados.size()) {
            return null;
        }
        return pedidosMostrados.get(fila);
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
