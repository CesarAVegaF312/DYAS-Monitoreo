package edu.unisabana.dyas.tienda.pedidos;

import edu.unisabana.dyas.tienda.comun.ProductoNoEncontradoException;
import edu.unisabana.dyas.tienda.comun.StockInsuficienteException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP del servicio de inventario.
 *
 * <p>Usa el {@link RestClient.Builder} que autoconfigura Spring Boot: ese builder ya trae la
 * instrumentación de Micrometer, que agrega el encabezado {@code traceparent} a cada petición.
 * Gracias a eso, la llamada a inventario aparece en Jaeger dentro de la misma traza del pedido.
 */
@Component
@Profile("pedidos")
public class InventarioClienteHttp implements InventarioCliente {

    private final RestClient restClient;

    public InventarioClienteHttp(RestClient.Builder builder, @Value("${inventario.url}") String inventarioUrl) {
        this.restClient = builder.baseUrl(inventarioUrl).build();
    }

    @Override
    public void reservar(String producto, int cantidad) {
        try {
            restClient.post()
                    .uri("/api/inventario/{producto}/reservas?cantidad={cantidad}", producto, cantidad)
                    .retrieve()
                    .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND), (request, response) -> {
                        throw new ProductoNoEncontradoException(producto);
                    })
                    .onStatus(status -> status.isSameCodeAs(HttpStatus.CONFLICT), (request, response) -> {
                        throw new StockInsuficienteException("Stock insuficiente de '" + producto + "'");
                    })
                    .onStatus(status -> status.isError(), (request, response) -> {
                        throw new InventarioNoDisponibleException(
                                "El inventario respondió " + response.getStatusCode().value(), null);
                    })
                    .toBodilessEntity();
        } catch (ResourceAccessException e) {
            throw new InventarioNoDisponibleException("El inventario no respondió: " + e.getMessage(), e);
        }
    }
}
