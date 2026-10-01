package cl.duocuc.view;

import cl.duocuc.controllers.EntregaController;
import cl.duocuc.controllers.PedidoController;
import cl.duocuc.controllers.RepartidorController;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;

/**
 * Ventana principal de SpeedFast. Es la única ventana de la aplicación: tiene un
 * menú lateral fijo a la izquierda y un área central con {@link CardLayout} donde
 * se muestran las secciones, por lo que navegar ya no abre ventanas nuevas.
 * <p>
 * Los controladores se crean una sola vez en {@code Main} y se comparten con todas
 * las secciones, de modo que todas trabajen sobre los mismos datos. La navegación
 * vive en la vista: los botones del menú solo cambian la tarjeta visible.
 */
public class VentanaPrincipal extends JFrame {

    /** Nombres de las tarjetas del {@link CardLayout}. */
    private static final String REPARTIDOR = "REPARTIDOR";
    private static final String PEDIDOS = "PEDIDOS";
    private static final String ENTREGAS = "ENTREGAS";

    /** Ancho fijo del menú lateral y alto común de sus botones. */
    private static final int ANCHO_MENU = 210;
    private static final int ALTO_BOTON = 44;
    private static final int SEPARACION_BOTONES = 10;

    /** Color de fondo del menú lateral. */
    private static final Color COLOR_MENU = new Color(236, 239, 244);

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel panelContenido = new JPanel(cardLayout);

    private final PanelRegistrarRepartidor panelRegistrarRepartidor;
    private final PanelPedidos panelPedidos;
    private final PanelEntregas panelEntregas;

    /**
     * Crea la ventana principal con los controladores que comparte toda la aplicación.
     *
     * @param pedidoController     controlador de pedidos compartido
     * @param repartidorController controlador de repartidores compartido
     * @param entregaController    controlador de entregas compartido
     */
    public VentanaPrincipal(PedidoController pedidoController,
                            RepartidorController repartidorController,
                            EntregaController entregaController) {
        this.panelRegistrarRepartidor =
                new PanelRegistrarRepartidor(repartidorController, this::refrescarVistas);
        // Los pedidos se registran, se asignan y se reparten desde su propio
        // listado, con diálogos y botones de la sección, así que el menú no necesita
        // secciones aparte para registrar ni para asignar pedidos.
        this.panelPedidos = new PanelPedidos(
                pedidoController, repartidorController, entregaController, this::refrescarVistas);
        this.panelEntregas = new PanelEntregas(entregaController, this::refrescarVistas);

        setTitle("SpeedFast - Sistema de Gestión de Entregas");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        panelContenido.add(panelRegistrarRepartidor, REPARTIDOR);
        panelContenido.add(panelPedidos, PEDIDOS);
        panelContenido.add(panelEntregas, ENTREGAS);

        add(crearEncabezado(), BorderLayout.NORTH);
        add(crearMenuLateral(), BorderLayout.WEST);
        add(panelContenido, BorderLayout.CENTER);
        add(crearPiePagina(), BorderLayout.SOUTH);

        mostrar(PEDIDOS);

        // El mínimo deja entrar completa la barra de cinco botones del CRUD: con
        // menos ancho, FlowLayout manda el último botón a una segunda fila que la
        // barra no tiene alto para mostrar.
        setMinimumSize(new Dimension(860, 500));
        setSize(900, 540);
        setLocationRelativeTo(null);
    }

    /**
     * Arma el título de la aplicación, que se mantiene visible en la parte superior.
     *
     * @return el panel del encabezado
     */
    private JPanel crearEncabezado() {
        JLabel titulo = new JLabel("SpeedFast - Sistema de Gestión de Entregas");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 20f));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)));
        panel.add(titulo, BorderLayout.WEST);
        return panel;
    }

    /**
     * Arma el menú lateral fijo con los botones de navegación, dispuestos
     * verticalmente y todos del mismo tamaño.
     *
     * @return el panel del menú lateral
     */
    private JPanel crearMenuLateral() {
        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBackground(COLOR_MENU);
        menu.setPreferredSize(new Dimension(ANCHO_MENU, 0));
        menu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(20, 15, 20, 15)));

        agregarBotonMenu(menu, "Repartidores", REPARTIDOR);
        agregarBotonMenu(menu, "Pedidos", PEDIDOS);
        agregarBotonMenu(menu, "Entregas", ENTREGAS);
        menu.add(Box.createVerticalGlue());
        agregarBoton(menu, "Salir", e -> salir());
        return menu;
    }

    /**
     * Agrega al menú lateral un botón que muestra una de las secciones.
     *
     * @param menu    panel del menú lateral
     * @param texto   texto del botón
     * @param tarjeta nombre de la tarjeta que abre el botón
     */
    private void agregarBotonMenu(JPanel menu, String texto, String tarjeta) {
        agregarBoton(menu, texto, e -> mostrar(tarjeta));
    }

    /**
     * Agrega al menú lateral un botón con el tamaño común de la barra y la acción
     * que debe ejecutar.
     *
     * @param menu   panel del menú lateral
     * @param texto  texto del botón
     * @param accion acción que ejecuta el botón
     */
    private void agregarBoton(JPanel menu, String texto, ActionListener accion) {
        JButton boton = new JButton(texto);
        boton.setAlignmentX(Component.CENTER_ALIGNMENT);
        boton.setMaximumSize(new Dimension(Integer.MAX_VALUE, ALTO_BOTON));
        boton.setPreferredSize(new Dimension(ANCHO_MENU, ALTO_BOTON));
        boton.setFocusPainted(false);
        boton.addActionListener(accion);

        menu.add(boton);
        menu.add(Box.createVerticalStrut(SEPARACION_BOTONES));
    }

    /**
     * Cierra la aplicación después de confirmar con el usuario.
     */
    private void salir() {
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Salir de SpeedFast?", "Confirmar salida", JOptionPane.YES_NO_OPTION);
        if (confirmacion == JOptionPane.YES_OPTION) {
            dispose();
            System.exit(0);
        }
    }

    /**
     * Arma la línea inferior con la identificación de la actividad.
     *
     * @return el panel del pie de página
     */
    private JPanel crearPiePagina() {
        JLabel etiquetaActividad =
                new JLabel("Desarrollo Orientado a Objetos II - Semana 8");
        etiquetaActividad.setFont(etiquetaActividad.getFont().deriveFont(11f));
        etiquetaActividad.setForeground(Color.GRAY);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(8, 20, 8, 20)));
        panel.add(etiquetaActividad, BorderLayout.WEST);
        return panel;
    }

    /**
     * Muestra una de las secciones en el área central, actualizando antes sus
     * datos para que el usuario vea siempre la información vigente.
     *
     * @param tarjeta nombre de la tarjeta que se quiere mostrar
     */
    private void mostrar(String tarjeta) {
        switch (tarjeta) {
            case REPARTIDOR -> panelRegistrarRepartidor.refrescar();
            case PEDIDOS -> panelPedidos.refrescar();
            case ENTREGAS -> panelEntregas.recargar();
            default -> { }
        }
        cardLayout.show(panelContenido, tarjeta);
    }

    /**
     * Actualiza las secciones después de un cambio en los datos, de modo que todas
     * las pantallas sigan mostrando lo mismo que los controladores.
     */
    private void refrescarVistas() {
        panelRegistrarRepartidor.refrescar();
        panelPedidos.refrescar();
        panelEntregas.recargar();
    }
}
