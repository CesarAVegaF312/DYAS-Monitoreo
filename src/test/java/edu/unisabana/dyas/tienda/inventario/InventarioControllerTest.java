package edu.unisabana.dyas.tienda.inventario;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.unisabana.dyas.tienda.comun.ManejadorErrores;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.Tracer;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Prueba la API HTTP del inventario sin levantar el servidor (MockMvc en modo standalone). */
class InventarioControllerTest {

    private SimuladorFallas fallas;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        InventarioService inventario = new InventarioService(new SimpleMeterRegistry(), Map.of("teclado", 5));
        fallas = new SimuladorFallas(0, 0, () -> 0.0, ms -> { });
        mvc = MockMvcBuilders.standaloneSetup(new InventarioController(inventario, fallas))
                .setControllerAdvice(new ManejadorErrores(Tracer.NOOP))
                .build();
    }

    @Test
    void consultarStock() throws Exception {
        mvc.perform(get("/api/inventario/teclado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(5));
    }

    @Test
    void reservarUnidades() throws Exception {
        mvc.perform(post("/api/inventario/teclado/reservas").param("cantidad", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockRestante").value(3));
    }

    @Test
    void erroresDeNegocioComoCodigosHttp() throws Exception {
        mvc.perform(get("/api/inventario/tablet")).andExpect(status().isNotFound());
        mvc.perform(post("/api/inventario/teclado/reservas").param("cantidad", "9"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/inventario/teclado/reservas").param("cantidad", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void configurarYReiniciarElSimuladorDeFallas() throws Exception {
        mvc.perform(post("/api/fallas").param("latenciaMs", "300").param("tasaError", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latenciaMs").value(300))
                .andExpect(jsonPath("$.tasaError").value(1.0));

        mvc.perform(get("/api/inventario/teclado")).andExpect(status().isInternalServerError());

        mvc.perform(delete("/api/fallas")).andExpect(jsonPath("$.tasaError").value(0.0));
        mvc.perform(get("/api/fallas")).andExpect(jsonPath("$.latenciaMs").value(0));
        mvc.perform(get("/api/inventario/teclado")).andExpect(status().isOk());
    }
}
