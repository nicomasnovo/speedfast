package cl.duocuc.interfaces;

/**
 * Contrato para los componentes capaces de cancelar un pedido.
 */
public interface Cancelable {

    /**
     * Cancela el pedido que el componente tenga en curso.
     */
    void cancelar();
}
