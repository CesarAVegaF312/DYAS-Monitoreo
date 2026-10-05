package edu.unisabana.dyas.tienda.pedidos;

import edu.unisabana.dyas.tienda.comun.ProductoNoEncontradoException;
import edu.unisabana.dyas.tienda.comun.StockInsuficienteException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Caso de uso "crear pedido": valida la solicitud, reserva el stock en el servicio de
 * inventario y guarda el pedido.
 *
 * <p>Publica una métrica propia de negocio: {@code pedidos_total{resultado="..."}}, un Counter
 * que cuenta los pedidos según su resultado.
 */
@Service
@Profile("pedidos")
public class PedidoService {

    static final int CANTIDAD_MAXIMA = 10;

    static final String CONFIRMADO = "confirmado";
    static final String SIN_STOCK = "sin_stock";
    static final String PRODUCTO_INEXISTENTE = "producto_inexistente";
    static final String ERROR_INVENTARIO = "error_inventario";

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    private final InventarioCliente inventario;
    private final MeterRegistry registry;
    private final Clock reloj;
    private final AtomicLong secuencia = new AtomicLong();
    private final Map<Long, Pedido> pedidos = new ConcurrentHashMap<>();

    @Autowired
    public PedidoService(InventarioCliente inventario, MeterRegistry registry) {
        this(inventario, registry, Clock.systemUTC());
    }

    PedidoService(InventarioCliente inventario, MeterRegistry registry, Clock reloj) {
        this.inventario = inventario;
        this.registry = registry;
        this.reloj = reloj;
    }

    public Pedido crear(SolicitudPedido solicitud) {
        validar(solicitud);
        String producto = solicitud.producto().trim().toLowerCase();
        try {
            inventario.reservar(producto, solicitud.cantidad());
        } catch (StockInsuficienteException e) {
            contar(SIN_STOCK);
            throw e;
        } catch (ProductoNoEncontradoException e) {
            contar(PRODUCTO_INEXISTENTE);
            throw e;
        } catch (InventarioNoDisponibleException e) {
            contar(ERROR_INVENTARIO);
            throw e;
        }
        Pedido pedido = new Pedido(secuencia.incrementAndGet(), producto, solicitud.cantidad(), Instant.now(reloj));
        pedidos.put(pedido.id(), pedido);
        contar(CONFIRMADO);
        log.info("Pedido {} confirmado: {} x {}", pedido.id(), pedido.cantidad(), pedido.producto());
        return pedido;
    }

    public Pedido buscar(long id) {
        Pedido pedido = pedidos.get(id);
        if (pedido == null) {
            throw new PedidoNoEncontradoException(id);
        }
        return pedido;
    }

    private void validar(SolicitudPedido solicitud) {
        if (solicitud == null || solicitud.producto() == null || solicitud.producto().isBlank()) {
            throw new IllegalArgumentException("El producto es obligatorio");
        }
        if (solicitud.cantidad() < 1 || solicitud.cantidad() > CANTIDAD_MAXIMA) {
            throw new IllegalArgumentException("La cantidad debe estar entre 1 y " + CANTIDAD_MAXIMA);
        }
    }

    private void contar(String resultado) {
        Counter.builder("pedidos")
                .description("Pedidos procesados, por resultado")
                .tag("resultado", resultado)
                .register(registry)
                .increment();
    }
}
