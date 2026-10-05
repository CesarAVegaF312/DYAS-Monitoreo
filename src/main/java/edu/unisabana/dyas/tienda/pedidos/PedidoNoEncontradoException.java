package edu.unisabana.dyas.tienda.pedidos;

/** No existe un pedido con el id solicitado (HTTP 404). */
public class PedidoNoEncontradoException extends RuntimeException {

    public PedidoNoEncontradoException(long id) {
        super("No existe el pedido " + id);
    }
}
