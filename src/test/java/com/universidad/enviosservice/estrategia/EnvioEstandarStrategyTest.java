package com.universidad.enviosservice.estrategia;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EnvioEstandarStrategyTest {

    private final EnvioEstandarStrategy estrategia = new EnvioEstandarStrategy();

    @ParameterizedTest(name = "{0} kg a {1} km cuesta {2}")
    @CsvSource({
        "3.0, 100.0, 50.0",    // pesoKg <= 5
        "5.0, 100.0, 50.0",    // limite: 5 kg todavia es el primer tramo
        "8.0, 100.0, 52.4",    // 5 < pesoKg <= 20
        "20.0, 100.0, 62.0",   // limite: 20 kg todavia es el segundo tramo
        "25.0, 100.0, 68.0"    // pesoKg > 20
    })
    void calculaCostoPorTramoDePeso(double pesoKg, double distanciaKm, double esperado) {
        assertEquals(esperado, estrategia.calcular(pesoKg, distanciaKm), 0.01);
    }

    @ParameterizedTest(name = "{1} km: recargo incluido en {2}")
    @CsvSource({
        "3.0, 500.0, 250.0",   // limite: 500 km exactos no tienen recargo
        "3.0, 501.0, 260.5"    // mas de 500 km suma 10
    })
    void aplicaRecargoSoloPorEncimaDe500Km(double pesoKg, double distanciaKm, double esperado) {
        assertEquals(esperado, estrategia.calcular(pesoKg, distanciaKm), 0.01);
    }
}
