package edu.unisabana.dyas.tienda.pedidos;

/** El servicio de inventario falló o no respondió a tiempo (HTTP 503). */
public class InventarioNoDisponibleException extends RuntimeException {

    public InventarioNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
