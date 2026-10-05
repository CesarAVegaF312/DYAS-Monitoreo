package edu.unisabana.dyas.tienda.inventario;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import java.util.function.LongConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Agrega latencia artificial y errores aleatorios a las operaciones del inventario,
 * para que los dashboards y las alertas tengan algo que mostrar.
 *
 * <p>Los valores iniciales vienen de las propiedades {@code fallas.latencia-ms} y
 * {@code fallas.tasa-error}, y se pueden cambiar en caliente con el endpoint {@code /api/fallas}.
 */
@Component
@Profile("inventario")
public class SimuladorFallas {

    static final long LATENCIA_MAXIMA_MS = 10_000;

    private static final Logger log = LoggerFactory.getLogger(SimuladorFallas.class);

    private final DoubleSupplier aleatorio;
    private final LongConsumer dormir;

    private volatile long latenciaMs;
    private volatile double tasaError;

    @Autowired
    public SimuladorFallas(@Value("${fallas.latencia-ms:0}") long latenciaMs,
                           @Value("${fallas.tasa-error:0}") double tasaError) {
        this(latenciaMs, tasaError, () -> ThreadLocalRandom.current().nextDouble(), SimuladorFallas::pausar);
    }

    /** Constructor para pruebas: permite controlar el azar y la espera. */
    SimuladorFallas(long latenciaMs, double tasaError, DoubleSupplier aleatorio, LongConsumer dormir) {
        this.aleatorio = aleatorio;
        this.dormir = dormir;
        configurar(latenciaMs, tasaError);
    }

    /**
     * Se invoca al inicio de cada operación del inventario: espera la latencia configurada
     * y, con probabilidad {@code tasaError}, lanza una {@link FallaSimuladaException}.
     */
    public void aplicar() {
        if (latenciaMs > 0) {
            dormir.accept(latenciaMs);
        }
        if (tasaError > 0 && aleatorio.getAsDouble() < tasaError) {
            throw new FallaSimuladaException();
        }
    }

    public void configurar(long latenciaMs, double tasaError) {
        if (latenciaMs < 0 || latenciaMs > LATENCIA_MAXIMA_MS) {
            throw new IllegalArgumentException("latenciaMs debe estar entre 0 y " + LATENCIA_MAXIMA_MS);
        }
        if (tasaError < 0 || tasaError > 1) {
            throw new IllegalArgumentException("tasaError debe estar entre 0 y 1");
        }
        this.latenciaMs = latenciaMs;
        this.tasaError = tasaError;
        log.info("Simulador de fallas: latenciaMs={}, tasaError={}", latenciaMs, tasaError);
    }

    public void reiniciar() {
        configurar(0, 0);
    }

    public long getLatenciaMs() {
        return latenciaMs;
    }

    public double getTasaError() {
        return tasaError;
    }

    private static void pausar(long milisegundos) {
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
