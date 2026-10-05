package edu.unisabana.dyas.tienda.comun;

import edu.unisabana.dyas.tienda.inventario.FallaSimuladaException;
import edu.unisabana.dyas.tienda.pedidos.InventarioNoDisponibleException;
import edu.unisabana.dyas.tienda.pedidos.PedidoNoEncontradoException;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.filter.ServerHttpObservationFilter;

/**
 * Traduce las excepciones de negocio a respuestas HTTP (RFC 9457, Problem Details).
 *
 * <p>Para los errores 5xx hace dos cosas más:
 * <ul>
 *   <li>Escribe una línea de log con nivel ERROR. Esa línea lleva el traceId de la petición,
 *       lo que permite buscar la traza completa en Jaeger.</li>
 *   <li>Registra la excepción en la observación HTTP de la petición y en el span actual. Como el
 *       error se maneja aquí, Spring no lo ve: sin esto la etiqueta {@code exception} de la métrica
 *       {@code http_server_requests} diría "none" y en Jaeger el span no aparecería marcado con error.</li>
 * </ul>
 */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    private final Tracer tracer;

    public ManejadorErrores(Tracer tracer) {
        this.tracer = tracer;
    }

    @ExceptionHandler({ProductoNoEncontradoException.class, PedidoNoEncontradoException.class})
    public ProblemDetail noEncontrado(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(StockInsuficienteException.class)
    public ProblemDetail sinStock(StockInsuficienteException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail solicitudInvalida(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(FallaSimuladaException.class)
    public ProblemDetail fallaSimulada(FallaSimuladaException ex, HttpServletRequest request) {
        return errorDelServidor(HttpStatus.INTERNAL_SERVER_ERROR, ex, request);
    }

    @ExceptionHandler(InventarioNoDisponibleException.class)
    public ProblemDetail inventarioNoDisponible(InventarioNoDisponibleException ex, HttpServletRequest request) {
        return errorDelServidor(HttpStatus.SERVICE_UNAVAILABLE, ex, request);
    }

    private ProblemDetail errorDelServidor(HttpStatus estado, RuntimeException ex, HttpServletRequest request) {
        log.error("Error al atender la solicitud: {}", ex.getMessage());
        ServerHttpObservationFilter.findObservationContext(request).ifPresent(contexto -> contexto.setError(ex));
        Span span = tracer.currentSpan();
        if (span != null) {
            span.error(ex);
        }
        return ProblemDetail.forStatusAndDetail(estado, ex.getMessage());
    }
}
