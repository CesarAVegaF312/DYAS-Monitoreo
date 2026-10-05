package edu.unisabana.dyas.tienda.inventario;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SimuladorFallasTest {

    private final List<Long> esperas = new ArrayList<>();

    private SimuladorFallas simulador(long latenciaMs, double tasaError, double valorAleatorio) {
        return new SimuladorFallas(latenciaMs, tasaError, () -> valorAleatorio, esperas::add);
    }

    @Test
    void sinFallasConfiguradasNoEsperaNiFalla() {
        SimuladorFallas simulador = simulador(0, 0, 0.0);

        assertDoesNotThrow(simulador::aplicar);
        assertEquals(List.of(), esperas);
    }

    @Test
    void aplicaLaLatenciaConfigurada() {
        simulador(800, 0, 0.5).aplicar();

        assertEquals(List.of(800L), esperas);
    }

    @Test
    void fallaCuandoElValorAleatorioQuedaPorDebajoDeLaTasa() {
        SimuladorFallas simulador = simulador(0, 0.3, 0.1);

        assertThrows(FallaSimuladaException.class, simulador::aplicar);
    }

    @Test
    void noFallaCuandoElValorAleatorioQuedaPorEncimaDeLaTasa() {
        SimuladorFallas simulador = simulador(0, 0.3, 0.7);

        assertDoesNotThrow(simulador::aplicar);
    }

    @Test
    void conTasaUnoSiempreFalla() {
        SimuladorFallas simulador = simulador(0, 1.0, 0.999);

        assertThrows(FallaSimuladaException.class, simulador::aplicar);
    }

    @ParameterizedTest
    @CsvSource({"-1, 0", "10001, 0", "0, -0.1", "0, 1.5"})
    void rechazaConfiguracionesFueraDeRango(long latenciaMs, double tasaError) {
        SimuladorFallas simulador = simulador(0, 0, 0.5);

        assertThrows(IllegalArgumentException.class, () -> simulador.configurar(latenciaMs, tasaError));
    }

    @Test
    void reiniciarDejaElServicioSinFallas() {
        SimuladorFallas simulador = simulador(500, 0.5, 0.0);

        simulador.reiniciar();

        assertEquals(0, simulador.getLatenciaMs());
        assertEquals(0.0, simulador.getTasaError());
        assertDoesNotThrow(simulador::aplicar);
    }

    @Test
    void elConstructorDeProduccionEsperaDeVerdad() {
        SimuladorFallas real = new SimuladorFallas(20, 0);

        long inicio = System.nanoTime();
        real.aplicar();
        long transcurridoMs = (System.nanoTime() - inicio) / 1_000_000;

        assertTrue(transcurridoMs >= 20, "esperó " + transcurridoMs + " ms");
    }
}
