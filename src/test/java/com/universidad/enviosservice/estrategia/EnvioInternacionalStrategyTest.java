package com.universidad.enviosservice.estrategia;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EnvioInternacionalStrategyTest {

    private final EnvioInternacionalStrategy estrategia = new EnvioInternacionalStrategy();

    @ParameterizedTest(name = "{0} kg a {1} km cuesta {2}")
    @CsvSource({
        "3.0, 100.0, 230.0",   // pesoKg <= 5
        "5.0, 100.0, 230.0",   // limite del primer tramo
        "8.0, 100.0, 239.0",   // 5 < pesoKg <= 20
        "20.0, 100.0, 275.0",  // limite del segundo tramo
        "25.0, 100.0, 297.5"   // pesoKg > 20
    })
    void calculaCostoPorTramoDePeso(double pesoKg, double distanciaKm, double esperado) {
        assertEquals(esperado, estrategia.calcular(pesoKg, distanciaKm), 0.01);
    }

    @ParameterizedTest(name = "{1} km: recargo incluido en {2}")
    @CsvSource({
        "3.0, 5000.0, 9050.0",  // 5000 km exactos no tienen recargo
        "3.0, 5001.0, 9151.8"   // mas de 5000 km suma 100
    })
    void aplicaRecargoSoloPorEncimaDe5000Km(double pesoKg, double distanciaKm, double esperado) {
        assertEquals(esperado, estrategia.calcular(pesoKg, distanciaKm), 0.01);
    }
}
