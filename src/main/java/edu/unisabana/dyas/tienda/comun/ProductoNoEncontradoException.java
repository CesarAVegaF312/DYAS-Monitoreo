package edu.unisabana.dyas.tienda.comun;

/** El producto solicitado no existe en el catálogo (HTTP 404). */
public class ProductoNoEncontradoException extends RuntimeException {

    public ProductoNoEncontradoException(String producto) {
        super("No existe el producto '" + producto + "'");
    }
}
