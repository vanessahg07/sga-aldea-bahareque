package co.edu.uniquindio.sga.domain.tarifa;

/** Puerto de persistencia de tarifas. Registrar una tarifa nunca borra las anteriores. */
public interface TarifaRepositorio {

    TablaTarifas tabla();

    void registrar(Tarifa tarifa);
}
