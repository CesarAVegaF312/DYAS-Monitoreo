package edu.unisabana.dyas.tienda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada. La misma aplicación se despliega como dos servicios distintos
 * según el perfil activo: {@code pedidos} o {@code inventario}.
 */
@SpringBootApplication
public class TiendaApplication {

    public static void main(String[] args) {
        SpringApplication.run(TiendaApplication.class, args);
    }
}
