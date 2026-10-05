package edu.unisabana.dyas.tienda.inventario;

/** Error provocado a propósito por el {@link SimuladorFallas} (HTTP 500). */
public class FallaSimuladaException extends RuntimeException {

    public FallaSimuladaException() {
        super("Falla simulada en el servicio de inventario");
    }
}
