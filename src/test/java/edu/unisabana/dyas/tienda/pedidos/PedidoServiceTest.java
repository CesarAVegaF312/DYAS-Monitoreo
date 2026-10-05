package edu.unisabana.dyas.tienda.pedidos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.unisabana.dyas.tienda.comun.ProductoNoEncontradoException;
import edu.unisabana.dyas.tienda.comun.StockInsuficienteException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PedidoServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-05T15:00:00Z");

    /** Doble de prueba del inventario: registra las llamadas y lanza la excepción configurada. */
    private static class InventarioFalso implements InventarioCliente {
        final List<String> reservas = new ArrayList<>();
        RuntimeException error;

        @Override
        public void reservar(String producto, int cantidad) {
            reservas.add(producto + ":" + cantidad);
            if (error != null) {
                throw error;
            }
        }
    }

    private InventarioFalso inventario;
    private SimpleMeterRegistry registry;
    private PedidoService servicio;

    @BeforeEach
    void setUp() {
        inventario = new InventarioFalso();
        registry = new SimpleMeterRegistry();
        servicio = new PedidoService(inventario, registry, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private double contador(String resultado) {
        var counter = registry.find("pedidos").tag("resultado", resultado).counter();
        return counter == null ? 0 : counter.count();
    }

    @Test
    void creaElPedidoReservandoEnElInventario() {
        Pedido pedido = servicio.crear(new SolicitudPedido(" Teclado ", 2));

        assertEquals(new Pedido(1, "teclado", 2, AHORA), pedido);
        assertEquals(List.of("teclado:2"), inventario.reservas);
        assertEquals(pedido, servicio.buscar(1));
    }

    @Test
    void cuentaLosPedidosConfirmadosEnLaMetricaPropia() {
        servicio.crear(new SolicitudPedido("teclado", 1));
        servicio.crear(new SolicitudPedido("mouse", 1));

        assertEquals(2.0, contador(PedidoService.CONFIRMADO));
    }

    @Test
    void sinStockPropagaElErrorYLoCuenta() {
        inventario.error = new StockInsuficienteException("sin unidades");

        assertThrows(StockInsuficienteException.class, () -> servicio.crear(new SolicitudPedido("portatil", 1)));
        assertEquals(1.0, contador(PedidoService.SIN_STOCK));
        assertEquals(0.0, contador(PedidoService.CONFIRMADO));
    }

    @Test
    void productoInexistentePropagaElErrorYLoCuenta() {
        inventario.error = new ProductoNoEncontradoException("tablet");

        assertThrows(ProductoNoEncontradoException.class, () -> servicio.crear(new SolicitudPedido("tablet", 1)));
        assertEquals(1.0, contador(PedidoService.PRODUCTO_INEXISTENTE));
    }

    @Test
    void inventarioCaidoPropagaElErrorYLoCuenta() {
        inventario.error = new InventarioNoDisponibleException("El inventario respondió 500", null);

        assertThrows(InventarioNoDisponibleException.class, () -> servicio.crear(new SolicitudPedido("mouse", 1)));
        assertEquals(1.0, contador(PedidoService.ERROR_INVENTARIO));
        assertThrows(PedidoNoEncontradoException.class, () -> servicio.buscar(1));
    }

    @ParameterizedTest
    @CsvSource(value = {"teclado, 0", "teclado, 11", "'  ', 1", "NULL, 1"}, nullValues = "NULL")
    void rechazaSolicitudesInvalidasSinLlamarAlInventario(String producto, int cantidad) {
        assertThrows(IllegalArgumentException.class, () -> servicio.crear(new SolicitudPedido(producto, cantidad)));
        assertEquals(List.of(), inventario.reservas);
    }

    @Test
    void rechazaUnaSolicitudNula() {
        assertThrows(IllegalArgumentException.class, () -> servicio.crear(null));
    }

    @Test
    void buscarUnPedidoInexistente() {
        assertThrows(PedidoNoEncontradoException.class, () -> servicio.buscar(99));
    }
}
