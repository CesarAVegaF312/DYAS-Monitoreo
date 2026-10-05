package edu.unisabana.dyas.tienda.comun;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.tracing.exporter.FinishedSpan;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TrazasConfigTest {

    private static FinishedSpan spanCon(Map<String, String> tags) {
        FinishedSpan span = mock(FinishedSpan.class);
        when(span.getTags()).thenReturn(tags);
        return span;
    }

    @Test
    void noExportaLasTrazasDeActuator() {
        assertFalse(new TrazasConfig().noExportarTrazasDeActuator()
                .isExportable(spanCon(Map.of("uri", "/actuator/prometheus"))));
    }

    @Test
    void exportaLasTrazasDeNegocio() {
        assertTrue(TrazasConfig.esExportable(spanCon(Map.of("uri", "/api/pedidos"))));
    }

    @Test
    void exportaLosSpansSinUri() {
        assertTrue(TrazasConfig.esExportable(spanCon(Map.of())));
    }
}
