package co.edu.uniquindio.sga.domain.alojamiento;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.Dinero;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;

/**
 * Parámetros configurables del alojamiento (Ficha, Anexo A). Ningún valor del
 * negocio que cambie de un alojamiento a otro vive como constante en el código.
 *
 * @param umbralEdadFacturable      edad desde la cual un ocupante genera cargo
 * @param horaEntrada               hora a partir de la cual se entrega el apartamento
 * @param horaSalida                hora límite para liberar el apartamento
 * @param horasPreparacion          horas requeridas entre una salida y la siguiente entrada
 * @param horasPlazoConfirmacion    horas que una reserva puede permanecer PENDIENTE
 * @param horaLimiteNoShow          hora del día de entrada a partir de la cual se puede declarar no-show
 * @param porcentajeAnticipo        porcentaje del valor de la estancia exigido para confirmar (0 = no se exige)
 * @param maxMascotas               máximo de mascotas por reserva (regla propia RP-03)
 * @param cargoMascota              cargo fijo por mascota por estancia (regla propia RP-03)
 * @param nochesDescuentoLargo      noches mínimas para el descuento por estancia larga (regla propia RP-02)
 * @param porcentajeDescuentoLargo  porcentaje de descuento por estancia larga (regla propia RP-02)
 * @param diasRecordatorioLlegada   días antes de la entrada en que se recuerda la llegada al titular (0 = no se envía)
 */
public record ParametrosAlojamiento(
        int umbralEdadFacturable,
        LocalTime horaEntrada,
        LocalTime horaSalida,
        int horasPreparacion,
        int horasPlazoConfirmacion,
        LocalTime horaLimiteNoShow,
        BigDecimal porcentajeAnticipo,
        int maxMascotas,
        Dinero cargoMascota,
        int nochesDescuentoLargo,
        BigDecimal porcentajeDescuentoLargo,
        int diasRecordatorioLlegada) {

    public ParametrosAlojamiento {
        ExcepcionNegocio.exigir(umbralEdadFacturable >= 0, CodigoError.PARAMETROS_INVALIDOS, "umbral de edad");
        ExcepcionNegocio.exigir(horaEntrada != null && horaSalida != null && horaLimiteNoShow != null,
                CodigoError.PARAMETROS_INVALIDOS, "horas de entrada, salida y no-show");
        ExcepcionNegocio.exigir(horasPreparacion >= 0, CodigoError.PARAMETROS_INVALIDOS, "tiempo de preparación");
        ExcepcionNegocio.exigir(horasPlazoConfirmacion > 0, CodigoError.PARAMETROS_INVALIDOS, "plazo de confirmación");
        ExcepcionNegocio.exigir(esPorcentaje(porcentajeAnticipo), CodigoError.PARAMETROS_INVALIDOS, "anticipo");
        ExcepcionNegocio.exigir(maxMascotas >= 0, CodigoError.PARAMETROS_INVALIDOS, "máximo de mascotas");
        ExcepcionNegocio.exigir(cargoMascota != null && !cargoMascota.esNegativo(),
                CodigoError.PARAMETROS_INVALIDOS, "cargo por mascota");
        ExcepcionNegocio.exigir(nochesDescuentoLargo > 0, CodigoError.PARAMETROS_INVALIDOS, "noches de descuento");
        ExcepcionNegocio.exigir(esPorcentaje(porcentajeDescuentoLargo),
                CodigoError.PARAMETROS_INVALIDOS, "porcentaje de descuento");
        ExcepcionNegocio.exigir(diasRecordatorioLlegada >= 0 && diasRecordatorioLlegada <= 30,
                CodigoError.PARAMETROS_INVALIDOS, "días del recordatorio de llegada (0 a 30)");
    }

    /**
     * Indica si un apartamento puede recibir una entrada el mismo día de una salida:
     * solo si el tiempo de preparación no excede la ventana entre la hora de salida
     * y la hora de entrada.
     */
    public boolean admiteEntradaMismoDiaDeSalida() {
        Duration ventana = Duration.between(horaSalida, horaEntrada);
        return !Duration.ofHours(horasPreparacion).minus(ventana).isPositive();
    }

    public boolean exigeAnticipo() {
        return porcentajeAnticipo.signum() > 0;
    }

    private static boolean esPorcentaje(BigDecimal valor) {
        return valor != null && valor.signum() >= 0 && valor.compareTo(BigDecimal.valueOf(100)) <= 0;
    }
}
