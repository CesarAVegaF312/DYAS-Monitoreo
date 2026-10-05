package edu.unisabana.dyas.tienda.pedidos;

/** Cuerpo de {@code POST /api/pedidos}: {@code {"producto": "teclado", "cantidad": 2}}. */
public record SolicitudPedido(String producto, int cantidad) {
}
