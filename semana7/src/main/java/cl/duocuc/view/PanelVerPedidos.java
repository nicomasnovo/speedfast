package cl.duocuc.view;

import cl.duocuc.controllers.PedidoController;
import cl.duocuc.model.Pedido;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/**
 * Sección con el listado de pedidos en una {@link JTable} de solo lectura. Es el
 * mismo listado que antes vivía en la ventana {@code VentanaListaPedidos}, ahora
 * como {@link JPanel} para poder mostrarse dentro del área central de la
 * {@link VentanaPrincipal}.
 * <p>
 * Los datos se toman siempre desde el {@link PedidoController}, sin mantener una
 * copia propia y sin consultar la base de datos desde la vista
 * (vista → controlador → ConexionBD → MySQL).
 */
public class PanelVerPedidos extends PanelSeccion {

    /** Columnas del listado. */
    private static final String[] COLUMNAS = {"ID", "Dirección", "Tipo", "Estado", "Repartidor"};

    private final PedidoController pedidoController;
    private final DefaultTableModel modeloTabla;
    private final JLabel etiquetaResumen = new JLabel(" ");

    /**
     * Crea el listado de pedidos.
     *
     * @param pedidoController controlador de pedidos compartido
     */
    public PanelVerPedidos(PedidoController pedidoController) {
        super("Ver Pedidos");
        this.pedidoController = pedidoController;
        this.modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable tabla = new JTable(modeloTabla);
        tabla.setFillsViewportHeight(true);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createEtchedBorder());
        add(scroll, BorderLayout.CENTER);
        add(crearPiePagina(), BorderLayout.SOUTH);

        refrescar();
    }

    /**
     * Arma el pie con el resumen y el botón Actualizar.
     *
     * @return el panel del pie de página
     */
    private JPanel crearPiePagina() {
        JButton botonActualizar = new JButton("Actualizar");
        botonActualizar.addActionListener(e -> recargarDesdeBaseDeDatos());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        botones.add(botonActualizar);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(etiquetaResumen, BorderLayout.CENTER);
        panel.add(botones, BorderLayout.EAST);
        return panel;
    }

    /**
     * Pide al controlador que vuelva a leer los pedidos desde MySQL y rearma la
     * tabla con lo que haya en la base de datos. La vista no consulta la base de
     * datos: solo llama al controlador.
     */
    public void recargarDesdeBaseDeDatos() {
        pedidoController.cargarPedidosDesdeDB();
        refrescar();
    }

    /**
     * Vuelve a consultar los pedidos al controlador y reconstruye las filas.
     */
    public void refrescar() {
        modeloTabla.setRowCount(0);
        for (Pedido pedido : pedidoController.getPedidos()) {
            String repartidor = pedido.getRepartidorAsignado();
            modeloTabla.addRow(new Object[]{
                    pedido.getId(),
                    pedido.getDireccionEntrega(),
                    pedido.getTipo(),
                    pedido.getEstado(),
                    repartidor != null ? repartidor : "-"
            });
        }
        etiquetaResumen.setText("Entregados: " + pedidoController.getEntregados()
                + " de " + pedidoController.getTotalPedidos() + " pedidos.");
    }
}
