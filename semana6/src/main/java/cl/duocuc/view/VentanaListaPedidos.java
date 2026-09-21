package cl.duocuc.view;

import cl.duocuc.controllers.PedidoController;
import cl.duocuc.model.Pedido;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/**
 * Listado de pedidos en una {@link JTable} de solo lectura. Reemplaza el listado
 * que antes se imprimía por consola y toma los datos siempre desde el
 * {@link PedidoController}, sin mantener su propia copia.
 */
public class VentanaListaPedidos extends JFrame {

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
    public VentanaListaPedidos(PedidoController pedidoController) {
        this.pedidoController = pedidoController;
        this.modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        setTitle("SpeedFast - Listado de Pedidos");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JTable tabla = new JTable(modeloTabla);
        tabla.setFillsViewportHeight(true);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        add(scroll, BorderLayout.CENTER);
        add(crearPiePagina(), BorderLayout.SOUTH);

        refrescar();
        setSize(640, 320);
        setLocationRelativeTo(null);
    }

    /**
     * Arma el pie con el resumen y el botón Actualizar.
     *
     * @return el panel del pie de página
     */
    private JPanel crearPiePagina() {
        JButton botonActualizar = new JButton("Actualizar");
        botonActualizar.addActionListener(e -> refrescar());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        botones.add(botonActualizar);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 15, 10, 10));
        panel.add(etiquetaResumen, BorderLayout.CENTER);
        panel.add(botones, BorderLayout.EAST);
        return panel;
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
