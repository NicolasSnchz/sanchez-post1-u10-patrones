package com.universidad.enviosservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServicioCalculoEnvioTest {

    private final ServicioCalculoEnvio servicio = new ServicioCalculoEnvio();

    @Test
    void calculaEnvioEstandarPesoBajo() {
        double costo = servicio.calcularCosto("ESTANDAR", 3.0, 100.0, "REGULAR", false);
        assertEquals(50.0, costo, 0.01);
    }

    @Test
    void aplicaDescuentoClientePremium() {
        double costo = servicio.calcularCosto("ESTANDAR", 3.0, 100.0, "PREMIUM", false);
        assertEquals(45.0, costo, 0.01);
    }

    @ParameterizedTest(name = "{1}, fragil={2}: {3}")
    @CsvSource({
        "ESTANDAR, CORPORATIVO, false, 42.5",   // descuento CORPORATIVO del 15 %
        "ESTANDAR, REGULAR, true, 57.5",        // recargo por fragilidad del 15 %
        "ESTANDAR, PREMIUM, true, 51.75"        // la fragilidad se suma antes del descuento PREMIUM
    })
    void aplicaAjustesPorTipoDeClienteYFragilidad(String metodoEnvio, String tipoCliente, boolean esFragil,
            double esperado) {
        assertEquals(esperado, servicio.calcularCosto(metodoEnvio, 3.0, 100.0, tipoCliente, esFragil), 0.01);
    }

    @ParameterizedTest(name = "{0} delega en su estrategia: {1}")
    @CsvSource({
        "EXPRESS, 156.0",
        "OVERNIGHT, 220.0",
        "INTERNACIONAL, 230.0"
    })
    void delegaEnLaEstrategiaDeCadaMetodo(String metodoEnvio, double esperado) {
        double costo = servicio.calcularCosto(metodoEnvio, 3.0, 100.0, "REGULAR", false);
        assertEquals(esperado, costo, 0.01);
    }

    @Test
    void rechazaMetodoDeEnvioNoSoportado() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> servicio.calcularCosto("MARITIMO", 3.0, 100.0, "REGULAR", false));
        assertEquals("Metodo de envio no soportado: MARITIMO", error.getMessage());
    }
}
