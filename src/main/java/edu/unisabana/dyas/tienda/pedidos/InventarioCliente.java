package edu.unisabana.dyas.tienda.pedidos;

/**
 * Puerto hacia el servicio de inventario. La implementación real hace una llamada HTTP
 * ({@link InventarioClienteHttp}); en las pruebas unitarias se reemplaza por un doble.
 */
public interface InventarioCliente {

    /**
     * Reserva unidades de un producto.
     *
     * @throws edu.unisabana.dyas.tienda.comun.ProductoNoEncontradoException si el producto no existe
     * @throws edu.unisabana.dyas.tienda.comun.StockInsuficienteException si no hay unidades suficientes
     * @throws InventarioNoDisponibleException si el inventario falla o no responde a tiempo
     */
    void reservar(String producto, int cantidad);
}
