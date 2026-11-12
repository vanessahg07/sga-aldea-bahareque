package co.edu.uniquindio.sga.domain.tarifa;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.Dinero;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Tarifas del alojamiento con su histórico. La tarifa vigente de un apartamento
 * en una temporada es la registrada con la fecha de vigencia más reciente.
 * Es un objeto de valor: agregar una tarifa produce una tabla nueva.
 */
public final class TablaTarifas {

    private final List<Tarifa> historico;

    public TablaTarifas(List<Tarifa> tarifas) {
        ExcepcionNegocio.exigir(tarifas != null && tarifas.stream().allMatch(t -> t != null),
                CodigoError.TARIFA_INVALIDA);
        this.historico = List.copyOf(tarifas);
    }

    /** Devuelve una tabla nueva con la tarifa agregada al histórico. */
    public TablaTarifas conTarifa(Tarifa tarifa) {
        ExcepcionNegocio.exigir(tarifa != null, CodigoError.TARIFA_INVALIDA);
        List<Tarifa> nuevas = new ArrayList<>(historico);
        nuevas.add(tarifa);
        return new TablaTarifas(nuevas);
    }

    public Optional<Tarifa> tarifaVigente(String codigoApartamento, String nombreTemporada) {
        return historico.stream()
                .filter(t -> t.codigoApartamento().equals(codigoApartamento)
                        && t.nombreTemporada().equals(nombreTemporada))
                .max(Comparator.comparing(Tarifa::vigenteDesde));
    }

    public Dinero valorPara(String codigoApartamento, String nombreTemporada) {
        return tarifaVigente(codigoApartamento, nombreTemporada)
                .map(Tarifa::valor)
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.TARIFAS_INCOMPLETAS,
                        codigoApartamento + " en " + nombreTemporada));
    }

    /** Todo apartamento activo debe tener tarifa en todas las temporadas. */
    public boolean tieneTarifasCompletas(String codigoApartamento, CalendarioTemporadas calendario) {
        return calendario.temporadas().stream()
                .allMatch(t -> tarifaVigente(codigoApartamento, t.nombre()).isPresent());
    }

    public List<Tarifa> historico(String codigoApartamento) {
        return historico.stream()
                .filter(t -> t.codigoApartamento().equals(codigoApartamento))
                .sorted(Comparator.comparing(Tarifa::vigenteDesde))
                .toList();
    }
}
