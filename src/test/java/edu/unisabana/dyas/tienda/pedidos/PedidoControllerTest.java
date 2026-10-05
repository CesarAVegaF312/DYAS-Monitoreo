package edu.unisabana.dyas.tienda.pedidos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.unisabana.dyas.tienda.comun.ManejadorErrores;
import edu.unisabana.dyas.tienda.comun.ProductoNoEncontradoException;
import edu.unisabana.dyas.tienda.comun.StockInsuficienteException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Prueba la API HTTP de pedidos sin levantar el servidor (MockMvc en modo standalone). */
class PedidoControllerTest {

    private MockMvc mvcCon(InventarioCliente inventario) {
        PedidoService servicio = new PedidoService(inventario, new SimpleMeterRegistry());
        return MockMvcBuilders.standaloneSetup(new PedidoController(servicio))
                .setControllerAdvice(new ManejadorErrores(Tracer.NOOP))
                .build();
    }

    private static String json(String producto, int cantidad) {
        return "{\"producto\":\"" + producto + "\",\"cantidad\":" + cantidad + "}";
    }

    @Test
    void crearYConsultarUnPedido() throws Exception {
        MockMvc mvc = mvcCon((producto, cantidad) -> { });

        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content(json("teclado", 2)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/pedidos/1"))
                .andExpect(jsonPath("$.producto").value("teclado"));

        mvc.perform(get("/api/pedidos/1")).andExpect(status().isOk()).andExpect(jsonPath("$.cantidad").value(2));
        mvc.perform(get("/api/pedidos/2")).andExpect(status().isNotFound());
    }

    @Test
    void solicitudInvalida() throws Exception {
        MockMvc mvc = mvcCon((producto, cantidad) -> { });

        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content(json("teclado", 50)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void erroresDelInventarioComoCodigosHttp() throws Exception {
        mvcCon((producto, cantidad) -> { throw new StockInsuficienteException("sin unidades"); })
                .perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content(json("portatil", 1)))
                .andExpect(status().isConflict());

        mvcCon((producto, cantidad) -> { throw new ProductoNoEncontradoException(producto); })
                .perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content(json("tablet", 1)))
                .andExpect(status().isNotFound());

        mvcCon((producto, cantidad) -> { throw new InventarioNoDisponibleException("caído", null); })
                .perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content(json("mouse", 1)))
                .andExpect(status().isServiceUnavailable());
    }
}
