package cl.duocuc.interfaces;

/**
 * Contrato para los componentes capaces de despachar un pedido.
 */
public interface Despachable {

    /**
     * Despacha el pedido que el componente tenga en curso.
     */
    void despachar();
}
