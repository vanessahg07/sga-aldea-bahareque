package co.edu.uniquindio.sga.domain.tarifa;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import co.edu.uniquindio.sga.domain.comun.RangoFechas;

import java.time.LocalDate;
import java.util.List;

/**
 * Periodo del calendario con tarifas propias. La temporada base no tiene rangos:
 * cubre todas las fechas no asignadas a otra temporada.
 *
 * @param estanciaMinimaNoches mínimo de noches exigido cuando la estancia toca esta
 *                             temporada (regla propia RP-01). 1 = sin restricción.
 */
public record Temporada(String nombre, boolean esBase, List<RangoFechas> rangos, int estanciaMinimaNoches) {

    public Temporada {
        ExcepcionNegocio.exigir(nombre != null && !nombre.isBlank(), CodigoError.TEMPORADA_INVALIDA, "nombre");
        ExcepcionNegocio.exigir(estanciaMinimaNoches >= 1, CodigoError.TEMPORADA_INVALIDA, "estancia mínima");
        rangos = rangos == null ? List.of() : List.copyOf(rangos);
        ExcepcionNegocio.exigir(esBase == rangos.isEmpty(), CodigoError.TEMPORADA_INVALIDA,
                "la temporada base no tiene rangos y las demás deben tener al menos uno");
        for (int i = 0; i < rangos.size(); i++) {
            for (int j = i + 1; j < rangos.size(); j++) {
                ExcepcionNegocio.exigir(!rangos.get(i).seSolapaCon(rangos.get(j)), CodigoError.TEMPORADAS_SOLAPADAS);
            }
        }
    }

    public static Temporada base(String nombre, int estanciaMinimaNoches) {
        return new Temporada(nombre, true, List.of(), estanciaMinimaNoches);
    }

    public static Temporada de(String nombre, int estanciaMinimaNoches, RangoFechas... rangos) {
        return new Temporada(nombre, false, List.of(rangos), estanciaMinimaNoches);
    }

    public boolean cubre(LocalDate noche) {
        return rangos.stream().anyMatch(r -> r.contiene(noche));
    }

    public boolean seSolapaCon(Temporada otra) {
        return rangos.stream().anyMatch(r -> otra.rangos.stream().anyMatch(r::seSolapaCon));
    }
}
