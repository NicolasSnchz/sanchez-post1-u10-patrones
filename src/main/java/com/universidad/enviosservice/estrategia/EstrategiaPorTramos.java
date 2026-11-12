package com.universidad.enviosservice.estrategia;

/**
 * Esqueleto comun de las estrategias (Template Method): todas cobran por kilometro, un cargo
 * fijo y un cargo por peso en tres tramos (hasta 5 kg, hasta 20 kg y mas de 20 kg). Cada
 * estrategia concreta solo define sus tarifas y sus ajustes propios por distancia.
 */
public abstract class EstrategiaPorTramos implements EstrategiaEnvio {

    private static final double LIMITE_TRAMO_BAJO_KG = 5;
    private static final double LIMITE_TRAMO_MEDIO_KG = 20;

    private final double tarifaPorKm;
    private final double cargoFijo;
    private final double tarifaTramoMedio;
    private final double tarifaTramoAlto;

    protected EstrategiaPorTramos(double tarifaPorKm, double cargoFijo, double tarifaTramoMedio,
            double tarifaTramoAlto) {
        this.tarifaPorKm = tarifaPorKm;
        this.cargoFijo = cargoFijo;
        this.tarifaTramoMedio = tarifaTramoMedio;
        this.tarifaTramoAlto = tarifaTramoAlto;
    }

    @Override
    public final double calcular(double pesoKg, double distanciaKm) {
        validarDistancia(distanciaKm);
        double costo = distanciaKm * tarifaPorKm + cargoFijo + cargoPorPeso(pesoKg);
        return ajustarPorDistancia(costo, distanciaKm);
    }

    /** Por defecto toda distancia es valida; una estrategia puede restringirla lanzando una excepcion. */
    protected void validarDistancia(double distanciaKm) {
        // Sin restriccion para la mayoria de los metodos de envio.
    }

    /** Recargos o factores que dependen de la distancia y se aplican sobre el costo por tramos. */
    protected abstract double ajustarPorDistancia(double costo, double distanciaKm);

    private double cargoPorPeso(double pesoKg) {
        if (pesoKg <= LIMITE_TRAMO_BAJO_KG) {
            return 0;
        }
        if (pesoKg <= LIMITE_TRAMO_MEDIO_KG) {
            return (pesoKg - LIMITE_TRAMO_BAJO_KG) * tarifaTramoMedio;
        }
        return (LIMITE_TRAMO_MEDIO_KG - LIMITE_TRAMO_BAJO_KG) * tarifaTramoMedio
            + (pesoKg - LIMITE_TRAMO_MEDIO_KG) * tarifaTramoAlto;
    }
}
