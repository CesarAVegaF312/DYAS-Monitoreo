package edu.unisabana.dyas.tienda.pedidos;

import java.net.URI;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** API del servicio de pedidos (perfil {@code pedidos}). */
@RestController
@Profile("pedidos")
public class PedidoController {

    private final PedidoService servicio;

    public PedidoController(PedidoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/api/pedidos")
    public ResponseEntity<Pedido> crear(@RequestBody SolicitudPedido solicitud) {
        Pedido pedido = servicio.crear(solicitud);
        return ResponseEntity.created(URI.create("/api/pedidos/" + pedido.id())).body(pedido);
    }

    @GetMapping("/api/pedidos/{id}")
    public Pedido buscar(@PathVariable long id) {
        return servicio.buscar(id);
    }
}
