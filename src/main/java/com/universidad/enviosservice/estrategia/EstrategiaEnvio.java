package com.universidad.enviosservice.estrategia;

/**
 * Strategy: cada metodo de envio calcula el costo base con su propia formula de tramos
 * de peso y recargo por distancia. El orquestador elige la estrategia una sola vez.
 */
@FunctionalInterface
public interface EstrategiaEnvio {
    double calcular(double pesoKg, double distanciaKm);
}
