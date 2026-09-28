package cl.duocuc.view;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Base de las secciones que se muestran en el área central de la
 * {@link VentanaPrincipal}. Cada sección es un {@link JPanel} con el mismo
 * título, márgenes y forma de armar los formularios, de modo que las cuatro
 * pantallas se vean iguales y no repitan ese código.
 */
abstract class PanelSeccion extends JPanel {

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
