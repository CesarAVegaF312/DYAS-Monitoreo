package edu.unisabana.dyas.tienda.inventario;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** API del servicio de inventario (perfil {@code inventario}). */
@RestController
@Profile("inventario")
public class InventarioController {

    /** Respuesta de la consulta de stock. */
    public record Stock(String producto, int stock) {
    }

    /** Respuesta de una reserva. */
    public record Reserva(String producto, int cantidad, int stockRestante) {
    }

    /** Configuración actual del simulador de fallas. */
    public record EstadoFallas(long latenciaMs, double tasaError) {
    }

    private final InventarioService inventario;
    private final SimuladorFallas fallas;

    public InventarioController(InventarioService inventario, SimuladorFallas fallas) {
        this.inventario = inventario;
        this.fallas = fallas;
    }

    @GetMapping("/api/inventario/{producto}")
    public Stock consultar(@PathVariable String producto) {
        fallas.aplicar();
        return new Stock(producto, inventario.consultar(producto));
    }

    @PostMapping("/api/inventario/{producto}/reservas")
    public Reserva reservar(@PathVariable String producto, @RequestParam int cantidad) {
        fallas.aplicar();
        int restante = inventario.reservar(producto, cantidad);
        return new Reserva(producto, cantidad, restante);
    }

    @GetMapping("/api/fallas")
    public EstadoFallas verFallas() {
        return new EstadoFallas(fallas.getLatenciaMs(), fallas.getTasaError());
    }

    @PostMapping("/api/fallas")
    public EstadoFallas configurarFallas(@RequestParam(defaultValue = "0") long latenciaMs,
                                         @RequestParam(defaultValue = "0") double tasaError) {
        fallas.configurar(latenciaMs, tasaError);
        return verFallas();
    }

    @DeleteMapping("/api/fallas")
    public EstadoFallas reiniciarFallas() {
        fallas.reiniciar();
        return verFallas();
    }
}
