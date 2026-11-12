package com.universidad.enviosservice.estrategia;

public class EnvioExpressStrategy extends EstrategiaPorTramos {

    public EnvioExpressStrategy() {
        super(1.2, 0, 1.5, 2.0);
    }

    @Override
    protected double ajustarPorDistancia(double costo, double distanciaKm) {
        double conRecargo = distanciaKm > 500 ? costo + 25 : costo;
        return conRecargo * 1.3;
    }
}
