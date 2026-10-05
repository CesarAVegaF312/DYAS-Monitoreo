package edu.unisabana.dyas.tienda.pedidos;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import edu.unisabana.dyas.tienda.comun.ProductoNoEncontradoException;
import edu.unisabana.dyas.tienda.comun.StockInsuficienteException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Verifica cómo se traducen las respuestas HTTP del inventario a excepciones de negocio. */
class InventarioClienteHttpTest {

    private static final String URL = "http://inventario:8082/api/inventario/teclado/reservas?cantidad=2";

    private MockRestServiceServer servidor;
    private InventarioClienteHttp cliente;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new InventarioClienteHttp(builder, "http://inventario:8082");
    }

    @Test
    void reservaExitosa() {
        servidor.expect(requestTo(URL)).andExpect(method(HttpMethod.POST)).andRespond(withSuccess());

        assertDoesNotThrow(() -> cliente.reservar("teclado", 2));
        servidor.verify();
    }

    @Test
    void un404SeTraduceEnProductoNoEncontrado() {
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(ProductoNoEncontradoException.class, () -> cliente.reservar("teclado", 2));
    }

    @Test
    void un409SeTraduceEnStockInsuficiente() {
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.CONFLICT));

        assertThrows(StockInsuficienteException.class, () -> cliente.reservar("teclado", 2));
    }

    @Test
    void un500SeTraduceEnInventarioNoDisponible() {
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(InventarioNoDisponibleException.class, () -> cliente.reservar("teclado", 2));
    }

    @Test
    void unTimeoutSeTraduceEnInventarioNoDisponible() {
        servidor.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThrows(InventarioNoDisponibleException.class, () -> cliente.reservar("teclado", 2));
    }
}
