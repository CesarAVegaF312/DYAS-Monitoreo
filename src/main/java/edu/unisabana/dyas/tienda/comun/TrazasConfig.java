package edu.unisabana.dyas.tienda.comun;

import io.micrometer.tracing.exporter.FinishedSpan;
import io.micrometer.tracing.exporter.SpanExportingPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Prometheus consulta {@code /actuator/prometheus} cada 5 segundos y Docker consulta
 * {@code /actuator/health} cada 10. Sin este filtro, Jaeger se llenaría de trazas de esas
 * peticiones de operación y costaría encontrar las de negocio.
 *
 * <p>Solo se dejan de <b>enviar las trazas</b>: las métricas de esas peticiones se siguen publicando.
 */
@Configuration(proxyBeanMethods = false)
public class TrazasConfig {

    @Bean
    SpanExportingPredicate noExportarTrazasDeActuator() {
        return TrazasConfig::esExportable;
    }

    static boolean esExportable(FinishedSpan span) {
        String uri = span.getTags().get("uri");
        return uri == null || !uri.startsWith("/actuator");
    }
}
