package cl.duocuc.view;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Base de las secciones que se muestran en el área central de la
 * {@link VentanaPrincipal}. Cada sección es un {@link JPanel} con el mismo
 * título, márgenes y forma de armar los formularios, de modo que las cuatro
 * pantallas se vean iguales y no repitan ese código.
 * <p>
 * También reúne los colores y la forma de los botones del CRUD, para que las tres
 * secciones que administran datos (pedidos, repartidores y entregas) los muestren
 * igual que la interfaz referencial de la actividad.
 */
abstract class PanelSeccion extends JPanel {

    /** Color del botón que crea registros. */
    protected static final Color COLOR_NUEVO = new Color(40, 167, 69);

    /** Color del botón que guarda o edita registros. */
    protected static final Color COLOR_EDITAR = new Color(52, 120, 220);

    /** Color del botón que elimina registros. */
    protected static final Color COLOR_ELIMINAR = new Color(220, 53, 69);

    /** Color del botón que vuelve a leer los datos. */
    protected static final Color COLOR_ACTUALIZAR = new Color(233, 236, 239);

    /** Color con que se muestra un botón deshabilitado. */
    private static final Color COLOR_DESHABILITADO = new Color(206, 212, 218);

    /**
     * Crea la sección con su título y los márgenes comunes.
     *
     * @param titulo texto que encabeza la sección
     */
    protected PanelSeccion(String titulo) {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel etiqueta = new JLabel(titulo);
        etiqueta.setFont(etiqueta.getFont().deriveFont(Font.BOLD, 18f));
        add(etiqueta, BorderLayout.NORTH);
    }

    /**
     * Deja un componente pegado a la parte superior del área central, para que
     * los formularios no queden estirados cuando la ventana crece.
     *
     * @param contenido componente que se quiere mostrar arriba
     */
    protected void agregarContenidoSuperior(JComponent contenido) {
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.add(contenido, BorderLayout.NORTH);
        add(contenedor, BorderLayout.CENTER);
    }

    /**
     * Crea un panel de formulario con las restricciones ya preparadas para
     * usarse con {@link #agregarFila(JPanel, GridBagConstraints, int, String, Component)}.
     *
     * @return el panel del formulario, vacío
     */
    protected JPanel crearFormulario() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));
        return panel;
    }

    /**
     * Crea las restricciones compartidas por las filas del formulario.
     *
     * @return las restricciones del {@link GridBagLayout}
     */
    protected GridBagConstraints crearRestricciones() {
        GridBagConstraints restricciones = new GridBagConstraints();
        restricciones.insets = new Insets(5, 5, 5, 5);
        restricciones.anchor = GridBagConstraints.LINE_START;
        return restricciones;
    }

    /**
     * Crea un botón del CRUD con el color y el icono de la interfaz referencial.
     * El icono se dibuja como un carácter delante del texto y solo se agrega si la
     * tipografía del botón sabe mostrarlo, para que en un equipo con otras fuentes
     * el botón no quede con un cuadro vacío.
     *
     * @param texto  texto del botón, por ejemplo {@code "Nuevo"}
     * @param icono  carácter que acompaña al texto, o {@code '\0'} si no lleva
     * @param fondo  color de fondo del botón
     * @return el botón ya configurado, sin acción asociada
     */
    protected JButton crearBotonAccion(String texto, char icono, Color fondo) {
        JButton boton = new JButton(texto);

        if (icono != '\0' && boton.getFont().canDisplay(icono)) {
            boton.setText(icono + "  " + texto);
        }

        // Sobre un fondo claro el texto negro se lee mejor; sobre los colores
        // fuertes, el blanco.
        boolean fondoClaro = fondo.equals(COLOR_ACTUALIZAR);
        boton.setBackground(fondo);
        boton.setForeground(fondoClaro ? Color.DARK_GRAY : Color.WHITE);
        boton.setFont(boton.getFont().deriveFont(Font.BOLD));

        // Necesario para que el color de fondo se pinte en los distintos
        // Look and Feel, incluido el de macOS.
        boton.setOpaque(true);
        boton.setBorderPainted(false);
        boton.setFocusPainted(false);
        boton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        // Como el fondo se pinta a mano, hay que apagarlo cuando el botón se
        // deshabilita; si no, seguiría viéndose de color y parecería disponible.
        boton.addPropertyChangeListener("enabled", cambio -> {
            boolean habilitado = Boolean.TRUE.equals(cambio.getNewValue());
            boton.setBackground(habilitado ? fondo : COLOR_DESHABILITADO);
            boton.setForeground(habilitado && !fondoClaro ? Color.WHITE : Color.DARK_GRAY);
        });
        return boton;
    }

    /**
     * Crea la barra donde van los botones del CRUD, pensada para ir justo encima
     * de la tabla como en la interfaz referencial.
     *
     * @param botones botones que se muestran, en el orden en que se quieren ver
     * @return el panel con los botones alineados a la izquierda
     */
    protected JPanel crearBarraDeAcciones(JButton... botones) {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        barra.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        for (JButton boton : botones) {
            barra.add(boton);
        }
        return barra;
    }

    /**
     * Agrega una fila etiqueta + campo al formulario.
     *
     * @param panel         panel del formulario
     * @param restricciones restricciones reutilizadas del {@link GridBagLayout}
     * @param fila          número de fila
     * @param texto         texto de la etiqueta
     * @param campo         componente de entrada
     */
    protected void agregarFila(JPanel panel, GridBagConstraints restricciones, int fila,
                               String texto, Component campo) {
        restricciones.gridx = 0;
        restricciones.gridy = fila;
        restricciones.fill = GridBagConstraints.NONE;
        restricciones.weightx = 0;
        panel.add(new JLabel(texto), restricciones);

        restricciones.gridx = 1;
        restricciones.fill = GridBagConstraints.HORIZONTAL;
        restricciones.weightx = 1;
        panel.add(campo, restricciones);
    }
}
