package co.edu.uniquindio.sga.domain.tarifa;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;

import java.time.LocalDate;
import java.util.List;

/**
 * Conjunto de temporadas del alojamiento. Garantiza que exista exactamente una
 * temporada base y que las demás no se solapen, de modo que toda fecha tenga
 * una y solo una temporada.
 */
public final class CalendarioTemporadas {

    private final List<Temporada> temporadas;
    private final Temporada base;

    public CalendarioTemporadas(List<Temporada> temporadas) {
        ExcepcionNegocio.exigir(temporadas != null, CodigoError.SIN_TEMPORADA_BASE);
        List<Temporada> bases = temporadas.stream().filter(Temporada::esBase).toList();
        ExcepcionNegocio.exigir(bases.size() == 1, CodigoError.SIN_TEMPORADA_BASE);

        List<Temporada> especiales = temporadas.stream().filter(t -> !t.esBase()).toList();
        for (int i = 0; i < especiales.size(); i++) {
            for (int j = i + 1; j < especiales.size(); j++) {
                ExcepcionNegocio.exigir(!especiales.get(i).seSolapaCon(especiales.get(j)),
                        CodigoError.TEMPORADAS_SOLAPADAS,
                        especiales.get(i).nombre() + " y " + especiales.get(j).nombre());
            }
        }
        this.temporadas = List.copyOf(temporadas);
        this.base = bases.getFirst();
    }

    /** Temporada que aplica a la noche indicada. Nunca queda una fecha sin temporada. */
    public Temporada temporadaDe(LocalDate noche) {
        return temporadas.stream()
                .filter(t -> !t.esBase() && t.cubre(noche))
                .findFirst()
                .orElse(base);
    }

    public List<Temporada> temporadas() {
        return temporadas;
    }

    public Temporada base() {
        return base;
    }
}
