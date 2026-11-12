package com.universidad.enviosservice.estrategia;

public class EnvioOvernightStrategy extends EstrategiaPorTramos {

    public EnvioOvernightStrategy() {
        super(2.0, 20, 2.5, 3.0);
    }

    @Override
    protected void validarDistancia(double distanciaKm) {
        if (distanciaKm > 300) {
            throw new IllegalArgumentException("OVERNIGHT no disponible para mas de 300 km");
        }
    }

    @Override
    protected double ajustarPorDistancia(double costo, double distanciaKm) {
        return costo;
    }
}
