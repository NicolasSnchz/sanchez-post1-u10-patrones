package com.universidad.enviosservice.estrategia;

public class EnvioEstandarStrategy extends EstrategiaPorTramos {

    public EnvioEstandarStrategy() {
        super(0.5, 0, 0.8, 1.2);
    }

    @Override
    protected double ajustarPorDistancia(double costo, double distanciaKm) {
        return distanciaKm > 500 ? costo + 10 : costo;
    }
}
