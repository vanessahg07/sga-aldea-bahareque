package co.edu.uniquindio.sga.domain.politica;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.Dinero;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * Política de cancelación del alojamiento en una versión concreta. Es inmutable:
 * actualizarla produce una nueva versión y la anterior se conserva. Cada reserva
 * queda ligada a la versión vigente al momento de su creación.
 */
public record PoliticaCancelacion(int version, LocalDateTime vigenteDesde, List<TramoCancelacion> tramos,
                                  BigDecimal porcentajeRetencionNoShow) {

    public PoliticaCancelacion {
        ExcepcionNegocio.exigir(version >= 1 && vigenteDesde != null, CodigoError.POLITICA_INVALIDA);
        ExcepcionNegocio.exigir(tramos != null && tramos.size() >= 2, CodigoError.POLITICA_INVALIDA,
                "se requieren al menos dos tramos de antelación");
        ExcepcionNegocio.exigir(tramos.stream().map(TramoCancelacion::diasAntelacionMinimos).distinct().count()
                == tramos.size(), CodigoError.POLITICA_INVALIDA, "tramos repetidos");
        ExcepcionNegocio.exigir(tramos.stream().anyMatch(t -> t.diasAntelacionMinimos() == 0),
                CodigoError.POLITICA_INVALIDA, "debe existir un tramo que cubra la antelación de 0 días");
        ExcepcionNegocio.exigir(porcentajeRetencionNoShow != null && porcentajeRetencionNoShow.signum() >= 0
                        && porcentajeRetencionNoShow.compareTo(BigDecimal.valueOf(100)) <= 0,
                CodigoError.POLITICA_INVALIDA, "retención por no-show");
        tramos = tramos.stream()
                .sorted(Comparator.comparingInt(TramoCancelacion::diasAntelacionMinimos).reversed())
                .toList();
    }

    /** Crea la siguiente versión de la política, sin alterar esta. */
    public PoliticaCancelacion nuevaVersion(LocalDateTime vigenteDesde, List<TramoCancelacion> tramos,
                                            BigDecimal porcentajeRetencionNoShow) {
        return new PoliticaCancelacion(version + 1, vigenteDesde, tramos, porcentajeRetencionNoShow);
    }

    public TramoCancelacion tramoPara(LocalDate fechaCancelacion, LocalDate fechaEntrada) {
        long diasAntelacion = ChronoUnit.DAYS.between(fechaCancelacion, fechaEntrada);
        return tramos.stream()
                .filter(t -> diasAntelacion >= t.diasAntelacionMinimos())
                .findFirst()
                .orElse(tramos.getLast());
    }

    /** Valor retenido al cancelar, redondeado al peso. */
    public Dinero retencionPorCancelacion(Dinero valorEstancia, LocalDate fechaCancelacion, LocalDate fechaEntrada) {
        return valorEstancia.porcentaje(tramoPara(fechaCancelacion, fechaEntrada).porcentajeRetencion()).redondear();
    }

    /** Valor retenido ante un no-show, redondeado al peso. */
    public Dinero retencionPorNoShow(Dinero valorEstancia) {
        return valorEstancia.porcentaje(porcentajeRetencionNoShow).redondear();
    }
}
