package edu.unisabana.dyas.tienda;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.unisabana.dyas.tienda.inventario.InventarioController;
import edu.unisabana.dyas.tienda.pedidos.PedidoController;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

/** Verifica que cada perfil arranca solo los componentes de su servicio. */
class TiendaApplicationTest {

    @Nested
    @SpringBootTest
    @ActiveProfiles("pedidos")
    class PerfilPedidos {

        @Autowired
        ApplicationContext contexto;

        @Test
        void soloCargaLaApiDePedidos() {
            assertEquals(1, contexto.getBeanNamesForType(PedidoController.class).length);
            assertEquals(0, contexto.getBeanNamesForType(InventarioController.class).length);
        }
    }

    @Nested
    @SpringBootTest
    @ActiveProfiles("inventario")
    class PerfilInventario {

        @Autowired
        ApplicationContext contexto;

        @Test
        void soloCargaLaApiDeInventario() {
            assertEquals(1, contexto.getBeanNamesForType(InventarioController.class).length);
            assertEquals(0, contexto.getBeanNamesForType(PedidoController.class).length);
        }
    }
}
