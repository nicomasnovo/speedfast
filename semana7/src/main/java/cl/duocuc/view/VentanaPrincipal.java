package cl.duocuc.view;

import cl.duocuc.controllers.PedidoController;
import cl.duocuc.controllers.RepartidorController;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.concurrent.ExecutionException;

/**
 * Ventana principal de SpeedFast. Reemplaza el menú que antes se mostraba por
 * consola y abre las demás vistas compartiendo los mismos controladores, de modo
 * que todas trabajen sobre los mismos datos.
 */
public class VentanaPrincipal extends JFrame {

    private final PedidoController pedidoController;
    private final RepartidorController repartidorController;

    private final JButton botonIniciarEntregas = new JButton("Iniciar Entregas");
    private final JLabel etiquetaEstado = new JLabel(" ");

    private VentanaRegistroPedido ventanaRegistro;
    private VentanaListaPedidos ventanaLista;
    private VentanaAsignarRepartidor ventanaAsignar;

    /**
     * Crea la ventana principal con los controladores que comparte toda la aplicación.
     *
     * @param pedidoController     controlador de pedidos compartido
     * @param repartidorController controlador de repartidores compartido
     */
    public VentanaPrincipal(PedidoController pedidoController,
                            RepartidorController repartidorController) {
        this.pedidoController = pedidoController;
        this.repartidorController = repartidorController;

        setTitle("SpeedFast - Menú Principal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        add(crearEncabezado(), BorderLayout.NORTH);
        add(crearBotonera(), BorderLayout.CENTER);
        add(crearPiePagina(), BorderLayout.SOUTH);

        setSize(480, 280);
        setLocationRelativeTo(null);
    }

    /**
     * Arma el título de la aplicación.
     *
     * @return el panel del encabezado
     */
    private JPanel crearEncabezado() {
        JLabel titulo = new JLabel("SpeedFast", JLabel.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 24f));

        JLabel subtitulo = new JLabel("Gestión de Entregas", JLabel.CENTER);

        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 5, 15));
        panel.add(titulo);
        panel.add(subtitulo);
        return panel;
    }

    /**
     * Arma los cuatro botones del menú principal.
     *
     * @return el panel con las acciones principales
     */
    private JPanel crearBotonera() {
        JButton botonRegistrar = new JButton("Registrar Pedido");
        JButton botonListar = new JButton("Listar Pedidos");
        JButton botonAsignar = new JButton("Asignar Repartidor");

        botonRegistrar.addActionListener(e -> abrirRegistroPedido());
        botonListar.addActionListener(e -> abrirListaPedidos());
        botonAsignar.addActionListener(e -> abrirAsignarRepartidor());
        botonIniciarEntregas.addActionListener(e -> iniciarEntregas());

        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        panel.add(botonRegistrar);
        panel.add(botonListar);
        panel.add(botonAsignar);
        panel.add(botonIniciarEntregas);
        return panel;
    }

    /**
     * Arma la línea inferior: el avance del despacho y la identificación de la actividad.
     *
     * @return el panel del pie de página
     */
    private JPanel crearPiePagina() {
        etiquetaEstado.setHorizontalAlignment(JLabel.CENTER);

        JLabel etiquetaActividad =
                new JLabel("Desarrollo Orientado a Objetos II - Semana 6", JLabel.CENTER);
        etiquetaActividad.setFont(etiquetaActividad.getFont().deriveFont(11f));
        etiquetaActividad.setForeground(Color.GRAY);

        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 15, 12, 15));
        panel.add(etiquetaEstado);
        panel.add(etiquetaActividad);
        return panel;
    }

    /**
     * Abre la ventana de registro de pedidos, reutilizándola si ya está abierta.
     */
    private void abrirRegistroPedido() {
        if (ventanaRegistro == null || !ventanaRegistro.isDisplayable()) {
            ventanaRegistro = new VentanaRegistroPedido(pedidoController, this::refrescarVistas);
        }
        mostrar(ventanaRegistro);
    }

    /**
     * Abre el listado de pedidos, reutilizándolo si ya está abierto.
     */
    private void abrirListaPedidos() {
        if (ventanaLista == null || !ventanaLista.isDisplayable()) {
            ventanaLista = new VentanaListaPedidos(pedidoController);
        }
        ventanaLista.refrescar();
        mostrar(ventanaLista);
    }

    /**
     * Abre la ventana de asignación de repartidores, reutilizándola si ya está abierta.
     */
    private void abrirAsignarRepartidor() {
        if (ventanaAsignar == null || !ventanaAsignar.isDisplayable()) {
            ventanaAsignar = new VentanaAsignarRepartidor(
                    pedidoController, repartidorController, this::refrescarVistas);
        }
        ventanaAsignar.recargarPendientes();
        mostrar(ventanaAsignar);
    }

    /**
     * Muestra una ventana secundaria y la trae al frente.
     *
     * @param ventana ventana que se quiere mostrar
     */
    private void mostrar(JFrame ventana) {
        ventana.setVisible(true);
        ventana.toFront();
        ventana.requestFocus();
    }

    /**
     * Lanza el reparto concurrente sin bloquear el Event Dispatch Thread: la
     * lógica existente del controlador se ejecuta en un {@link SwingWorker} y el
     * resultado se muestra cuando termina.
     */
    private void iniciarEntregas() {
        botonIniciarEntregas.setEnabled(false);
        etiquetaEstado.setText("Reparto en curso...");

        new SwingWorker<String, Void>() {

            @Override
            protected String doInBackground() throws Exception {
                return repartidorController.iniciarEntregas();
            }

            @Override
            protected void done() {
                botonIniciarEntregas.setEnabled(true);
                etiquetaEstado.setText(" ");
                try {
                    String resumen = get();
                    // Los hilos de reparto cambiaron el estado de los pedidos en
                    // memoria; se guardan en MySQL para que el avance persista.
                    pedidoController.guardarEstados();
                    refrescarVistas();
                    JOptionPane.showMessageDialog(VentanaPrincipal.this, resumen,
                            "Resumen del despacho", JOptionPane.INFORMATION_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause() != null ? e.getCause() : e;
                    JOptionPane.showMessageDialog(VentanaPrincipal.this, causa.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /**
     * Actualiza las ventanas abiertas después de un cambio en los datos.
     */
    private void refrescarVistas() {
        if (ventanaLista != null && ventanaLista.isDisplayable()) {
            ventanaLista.refrescar();
        }
        if (ventanaAsignar != null && ventanaAsignar.isDisplayable()) {
            ventanaAsignar.recargarPendientes();
        }
    }
}
