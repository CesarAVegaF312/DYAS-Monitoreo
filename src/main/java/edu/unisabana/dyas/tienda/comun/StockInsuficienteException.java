package edu.unisabana.dyas.tienda.comun;

/** No hay unidades suficientes para atender la solicitud (HTTP 409). */
public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException(String producto, int solicitadas, int disponibles) {
        super("Stock insuficiente de '" + producto + "': se pidieron " + solicitadas
                + " y hay " + disponibles);
    }

    public StockInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
