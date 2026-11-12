package com.universidad.enviosservice.estrategia;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EnvioExpressStrategyTest {

    private final EnvioExpressStrategy estrategia = new EnvioExpressStrategy();

    @ParameterizedTest(name = "{0} kg a {1} km cuesta {2}")
    @CsvSource({
        "3.0, 100.0, 156.0",    // pesoKg <= 5, por 1.3
        "5.0, 100.0, 156.0",    // limite del primer tramo
        "8.0, 100.0, 161.85",   // 5 < pesoKg <= 20
        "20.0, 100.0, 185.25",  // limite del segundo tramo
        "25.0, 100.0, 198.25"   // pesoKg > 20
    })
    void calculaCostoPorTramoDePeso(double pesoKg, double distanciaKm, double esperado) {
        assertEquals(esperado, estrategia.calcular(pesoKg, distanciaKm), 0.01);
    }

    @ParameterizedTest(name = "{1} km: recargo incluido en {2}")
    @CsvSource({
        "3.0, 500.0, 780.0",    // 500 km exactos no tienen recargo
        "3.0, 501.0, 814.06"    // mas de 500 km suma 25 antes del factor 1.3
    })
    void aplicaRecargoSoloPorEncimaDe500Km(double pesoKg, double distanciaKm, double esperado) {
        assertEquals(esperado, estrategia.calcular(pesoKg, distanciaKm), 0.01);
    }
}
