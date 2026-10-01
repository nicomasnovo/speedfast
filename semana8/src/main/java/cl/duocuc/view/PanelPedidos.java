package cl.duocuc.view;

import cl.duocuc.controllers.EntregaController;
import cl.duocuc.controllers.PedidoController;
import cl.duocuc.controllers.RepartidorController;
import cl.duocuc.exception.PersistenciaException;
import cl.duocuc.model.EstadoPedido;
import cl.duocuc.model.Pedido;
import cl.duocuc.model.TipoPedido;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Sección con el listado de pedidos en una {@link JTable} de solo lectura y todas
 * las operaciones que se hacen sobre ellos: registrar, asignar a un repartidor,
 * editar, eliminar, filtrar, volver a leer desde MySQL e iniciar el reparto.
 *
 * <p>
 * El pedido sobre el que se trabaja es siempre el de la fila seleccionada, así que
 * asignar un repartidor ya no necesita su propia sección ni un desplegable de
 * pedidos: basta elegir la fila y pulsar Asignar. El botón Iniciar Entregas, en
 * cambio, es una acción general sobre toda la zona de carga, por lo que va en el
 * pie de la sección y no en la barra del CRUD.
 *
 * <p>
 * Los datos se toman siempre desde el {@link PedidoController}, sin mantener una
 * copia propia y sin consultar la base de datos desde la vista
 * (vista → controlador → DAO → ConexionBD → MySQL). Los mensajes, incluidos los
 * errores de base de datos que llegan como {@link PersistenciaException}, se
 * muestran aquí con {@link JOptionPane}.
 *
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

    /** Necesario para asignar pedidos y para lanzar el reparto concurrente. */
    private final RepartidorController repartidorController;

    /** Necesario para anotar como entregas los pedidos que reparte el reparto. */
    private final EntregaController entregaController;

    /** Aviso que la ventana principal usa para refrescar las demás secciones. */
    private final Runnable alCambiar;

    private final DefaultTableModel modeloTabla;
    private final JTable tabla;
    private final JLabel etiquetaResumen = new JLabel(" ");

    /** Mensaje de avance que se muestra mientras corre el reparto. */
    private final JLabel etiquetaEstado = new JLabel(" ");

    private final JButton botonIniciarEntregas =
            crearBotonAccion("Iniciar Entregas", '▶', COLOR_EDITAR);

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
     * @param pedidoController     controlador de pedidos compartido
     * @param repartidorController controlador de repartidores compartido
     * @param entregaController    controlador de entregas compartido
     * @param alCambiar            acción que se ejecuta cuando cambian los datos
     */
    public PanelPedidos(PedidoController pedidoController,
                        RepartidorController repartidorController,
                        EntregaController entregaController,
                        Runnable alCambiar) {
        super("Gestión de Pedidos");
        this.pedidoController = pedidoController;
        this.repartidorController = repartidorController;
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

        llenarFiltros();

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createEtchedBorder());

        // Los botones del CRUD y los filtros van sobre la tabla
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
        JButton botonAsignar = crearBotonAccion("Asignar", '→', COLOR_EDITAR);
        JButton botonEditar = crearBotonAccion("Editar", '✎', COLOR_EDITAR);
        JButton botonEliminar = crearBotonAccion("Eliminar", '✕', COLOR_ELIMINAR);
        JButton botonActualizar = crearBotonAccion("Actualizar", '↻', COLOR_ACTUALIZAR);

        botonNuevo.addActionListener(e -> abrirVentanaNuevoPedido());
        botonAsignar.addActionListener(e -> asignar());
        botonEditar.addActionListener(e -> editar());
        botonEliminar.addActionListener(e -> eliminar());
        botonActualizar.addActionListener(e -> recargarDesdeBaseDeDatos());

        return new JButton[]{
                botonNuevo, botonAsignar, botonEditar, botonEliminar, botonActualizar};
    }

    /**
     * Abre el formulario {@link PanelRegistrarPedido} dentro de un diálogo
     * emergente modal, para registrar un pedido sin salir del listado.
     */
    private void abrirVentanaNuevoPedido() {
        Window ventanaPadre = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(ventanaPadre, "Registrar Nuevo Pedido", Dialog.ModalityType.APPLICATION_MODAL);

        PanelRegistrarPedido panelRegistrar = new PanelRegistrarPedido(pedidoController, () -> {
            // Tras guardar: se cierra el diálogo, se rearma la tabla y se avisa a
            // la ventana principal para que el resto de las secciones también vea
            // el pedido nuevo.
            dialog.dispose();
            refrescar();
            avisarCambio();
        });

        dialog.getContentPane().add(panelRegistrar);
        dialog.pack();
        dialog.setLocationRelativeTo(ventanaPadre);
        dialog.setVisible(true);
    }

    /**
     * Arma el pie con el resumen de lo que se está mostrando y, a la derecha, el
     * inicio del reparto con su mensaje de avance. El reparto va aquí porque no
     * depende de la fila seleccionada: actúa sobre todos los pedidos asignados.
     *
     * @return el panel del pie de página
     */
    private JPanel crearPiePagina() {
        botonIniciarEntregas.addActionListener(e -> iniciarEntregas());

        JPanel contenedorBoton = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        contenedorBoton.add(etiquetaEstado);
        contenedorBoton.add(botonIniciarEntregas);

        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.add(etiquetaResumen, BorderLayout.WEST);
        panel.add(contenedorBoton, BorderLayout.EAST);
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
     * Asigna el pedido seleccionado a un repartidor. La reserva la sigue haciendo
     * {@link RepartidorController} sobre la zona de carga, igual que cuando la
     * asignación tenía su propia sección; aquí solo se elige el repartidor, porque
     * el pedido es el de la fila seleccionada.
     */
    private void asignar() {
        Pedido pedido = getPedidoSeleccionado();
        if (pedido == null) {
            mostrarError("Debe seleccionar un pedido de la tabla para asignarlo.");
            return;
        }

        List<String> nombres = repartidorController.getNombresRepartidores();
        if (nombres.isEmpty()) {
            mostrarError("No hay repartidores registrados: registre uno en la sección"
                    + " Registrar Repartidor antes de asignar el pedido.");
            return;
        }

        JComboBox<String> comboRepartidores = new JComboBox<>(nombres.toArray(new String[0]));

        JPanel formulario = crearFormulario();
        GridBagConstraints restricciones = crearRestricciones();
        agregarFila(formulario, restricciones, 0, "Pedido:",
                new JLabel("#" + pedido.getId() + " - " + pedido.getDireccionEntrega()));
        agregarFila(formulario, restricciones, 1, "Repartidor:", comboRepartidores);

        int opcion = JOptionPane.showConfirmDialog(this, formulario,
                "Asignar pedido #" + pedido.getId(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) {
            return;
        }

        String repartidor = (String) comboRepartidores.getSelectedItem();
        try {
            repartidorController.asignarRepartidor(pedido.getId(), repartidor);

            // La asignación queda hecha en la zona de carga (en memoria); se guarda
            // en MySQL para que no se pierda al recargar los pedidos.
            if (!pedidoController.editarPedido(pedido)) {
                mostrarError("No se pudo guardar la asignación del pedido #" + pedido.getId()
                        + ": puede que ya no exista en la base de datos.");
            } else {
                JOptionPane.showMessageDialog(this, "Pedido #" + pedido.getId()
                        + " asignado a " + repartidor + ".");
            }
        } catch (IllegalArgumentException | IllegalStateException | PersistenciaException ex) {
            // Por ejemplo, cuando el pedido ya no está pendiente o ya fue retirado
            // por un repartidor: se avisa y la tabla se vuelve a armar.
            mostrarError(ex.getMessage());
        }
        refrescar();
        avisarCambio();
    }

    /**
     * Lanza el reparto concurrente sin bloquear el Event Dispatch Thread: la lógica
     * existente del controlador se ejecuta en un {@link SwingWorker} y el resultado
     * se muestra cuando termina.
     * <p>
     * Al terminar el reparto se guardan los estados en MySQL y los pedidos que
     * quedaron entregados se anotan en la tabla {@code entrega}, de modo que la
     * sección Entregas liste todos los pedidos entregados, los que se registran a
     * mano y los que reparten los repartidores.
     */
    private void iniciarEntregas() {
        botonIniciarEntregas.setEnabled(false);
        etiquetaEstado.setText("Reparto en curso...");

        new SwingWorker<String, Void>() {

            @Override
            protected String doInBackground() throws Exception {
                String resumen = repartidorController.iniciarEntregas();

                // Los hilos de reparto cambiaron el estado de los pedidos en
                // memoria; se guardan en MySQL para que el avance persista.
                pedidoController.guardarEstados();

                // Cada pedido entregado queda además anotado en la tabla entrega,
                // que es la lista de los pedidos que llegaron a su destino. Las dos
                // escrituras van en este hilo y no en done(), para no dejar la
                // interfaz esperando a la base de datos.
                int anotadas = entregaController.registrarEntregasDelReparto();

                return resumen + "\n\nEntregas anotadas en la sección Entregas: " + anotadas;
            }

            @Override
            protected void done() {
                botonIniciarEntregas.setEnabled(true);
                etiquetaEstado.setText(" ");
                try {
                    String resumen = get();
                    refrescar();
                    avisarCambio();
                    JOptionPane.showMessageDialog(PanelPedidos.this, resumen,
                            "Resumen del despacho", JOptionPane.INFORMATION_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    // Aquí llegan también los errores de MySQL al guardar los
                    // estados o al anotar las entregas, envueltos por el SwingWorker.
                    Throwable causa = e.getCause() != null ? e.getCause() : e;
                    mostrarError(causa.getMessage());
                }
            }
        }.execute();
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
     * Avisa a la ventana principal que los datos cambiaron, para que las demás
     * secciones también se pongan al día.
     */
    private void avisarCambio() {
        if (alCambiar != null) {
            alCambiar.run();
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