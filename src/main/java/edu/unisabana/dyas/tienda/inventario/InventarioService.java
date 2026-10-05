package edu.unisabana.dyas.tienda.inventario;

import edu.unisabana.dyas.tienda.comun.ProductoNoEncontradoException;
import edu.unisabana.dyas.tienda.comun.StockInsuficienteException;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Inventario en memoria. Publica una métrica propia:
 * {@code inventario_stock{producto="..."}}, un Gauge con las unidades disponibles de cada producto.
 */
@Service
@Profile("inventario")
public class InventarioService {

    static final Map<String, Integer> STOCK_INICIAL = Map.of(
            "teclado", 50_000,
            "mouse", 50_000,
            "monitor", 20_000,
            "audifonos", 30_000,
            "portatil", 0);

    private static final Logger log = LoggerFactory.getLogger(InventarioService.class);

    private final Map<String, AtomicInteger> stock = new ConcurrentHashMap<>();

    @Autowired
    public InventarioService(MeterRegistry registry) {
        this(registry, STOCK_INICIAL);
    }

    InventarioService(MeterRegistry registry, Map<String, Integer> stockInicial) {
        stockInicial.forEach((producto, unidades) -> {
            AtomicInteger disponible = new AtomicInteger(unidades);
            stock.put(producto, disponible);
            Gauge.builder("inventario.stock", disponible, AtomicInteger::get)
                    .description("Unidades disponibles por producto")
                    .tag("producto", producto)
                    .register(registry);
        });
    }

    public int consultar(String producto) {
        return buscar(producto).get();
    }

    /**
     * Descuenta {@code cantidad} unidades del producto.
     *
     * @return las unidades que quedan después de la reserva
     */
    public int reservar(String producto, int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
        AtomicInteger disponible = buscar(producto);
        int actual;
        do {
            actual = disponible.get();
            if (actual < cantidad) {
                log.warn("Reserva rechazada: {} unidades de '{}', disponibles {}", cantidad, producto, actual);
                throw new StockInsuficienteException(producto, cantidad, actual);
            }
        } while (!disponible.compareAndSet(actual, actual - cantidad));
        int restante = actual - cantidad;
        log.info("Reservadas {} unidades de '{}', quedan {}", cantidad, producto, restante);
        return restante;
    }

    private AtomicInteger buscar(String producto) {
        AtomicInteger disponible = stock.get(producto);
        if (disponible == null) {
            throw new ProductoNoEncontradoException(producto);
        }
        return disponible;
    }
}
