package com.universidad.enviosservice.estrategia;

public class EnvioInternacionalStrategy extends EstrategiaPorTramos {

    public EnvioInternacionalStrategy() {
        super(1.8, 50, 3.0, 4.5);
    }

    @Override
    protected double ajustarPorDistancia(double costo, double distanciaKm) {
        return distanciaKm > 5000 ? costo + 100 : costo;
    }
}
