package edu.unisabana.dyas.tienda.pedidos;

import java.time.Instant;

/** Pedido confirmado. */
public record Pedido(long id, String producto, int cantidad, Instant fecha) {
}
