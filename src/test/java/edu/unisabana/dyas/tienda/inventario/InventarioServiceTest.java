package edu.unisabana.dyas.tienda.inventario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.unisabana.dyas.tienda.comun.ProductoNoEncontradoException;
import edu.unisabana.dyas.tienda.comun.StockInsuficienteException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class InventarioServiceTest {

    private SimpleMeterRegistry registry;
    private InventarioService inventario;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        inventario = new InventarioService(registry, Map.of("teclado", 10, "portatil", 0));
    }

    @Test
    void consultaElStockDeUnProducto() {
        assertEquals(10, inventario.consultar("teclado"));
    }

    @Test
    void reservarDescuentaUnidadesYDevuelveLoQueQueda() {
        int restante = inventario.reservar("teclado", 3);

        assertEquals(7, restante);
        assertEquals(7, inventario.consultar("teclado"));
    }

    @Test
    void rechazaLaReservaSiNoHayStockSuficienteSinTocarElInventario() {
        assertThrows(StockInsuficienteException.class, () -> inventario.reservar("teclado", 11));
        assertThrows(StockInsuficienteException.class, () -> inventario.reservar("portatil", 1));

        assertEquals(10, inventario.consultar("teclado"));
    }

    @Test
    void productoInexistente() {
        assertThrows(ProductoNoEncontradoException.class, () -> inventario.consultar("tablet"));
        assertThrows(ProductoNoEncontradoException.class, () -> inventario.reservar("tablet", 1));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rechazaCantidadesNoPositivas(int cantidad) {
        assertThrows(IllegalArgumentException.class, () -> inventario.reservar("teclado", cantidad));
    }

    @Test
    void publicaElStockComoGaugeConLaEtiquetaDelProducto() {
        inventario.reservar("teclado", 4);

        double valor = registry.get("inventario.stock").tag("producto", "teclado").gauge().value();
        assertEquals(6.0, valor);
    }

    @Test
    void elConstructorDeProduccionCargaElCatalogoInicial() {
        InventarioService real = new InventarioService(new SimpleMeterRegistry());

        assertEquals(InventarioService.STOCK_INICIAL.get("monitor"), real.consultar("monitor"));
        assertEquals(0, real.consultar("portatil"));
    }
}
