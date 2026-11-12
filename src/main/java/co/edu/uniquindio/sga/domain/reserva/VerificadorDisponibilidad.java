package co.edu.uniquindio.sga.domain.reserva;

import co.edu.uniquindio.sga.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import co.edu.uniquindio.sga.domain.tarifa.Temporada;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Valida que un apartamento pueda venderse para una estancia. Aplica, en el orden
 * exigido por el enunciado (7.5), las seis condiciones de creación y luego las reglas
 * propias del alojamiento:
 * <ol>
 *     <li>Fecha de salida posterior a la de entrada (RN-03, garantizada por {@link Estancia}).</li>
 *     <li>Fecha de entrada no anterior a hoy (RN-04).</li>
 *     <li>Apartamento activo y con tarifas completas.</li>
 *     <li>Capacidad suficiente para el total de ocupantes (RN-02).</li>
 *     <li>Sin solapamiento con reservas activas (RN-01) ni con bloqueos (RN-07).</li>
 *     <li>Tiempo de preparación respecto de las estancias contiguas (RN-20).</li>
 * </ol>
 * Reglas propias: estancia mínima por temporada (RP-01) y mascotas (RP-03).
 */
public class VerificadorDisponibilidad {

    /**
     * @param idReservaExcluida reserva que se está modificando, para no chocar consigo misma; puede ser null
     */
    public void verificar(OcupacionApartamento ocupacion, Estancia estancia, int totalOcupantes, int mascotas,
                          ContextoReserva contexto, LocalDate hoy, String idReservaExcluida) {
        Apartamento apartamento = ocupacion.apartamento();
        ParametrosAlojamiento parametros = contexto.parametros();

        // 2. RN-04
        ExcepcionNegocio.exigir(!estancia.fechaEntrada().isBefore(hoy), CodigoError.FECHA_ENTRADA_PASADA);

        // 3. Activo y con tarifas completas
        ExcepcionNegocio.exigir(apartamento.estaALaVenta(), CodigoError.APARTAMENTO_INACTIVO, apartamento.getCodigo());
        ExcepcionNegocio.exigir(contexto.tarifas().tieneTarifasCompletas(apartamento.getCodigo(), contexto.calendario()),
                CodigoError.TARIFAS_INCOMPLETAS, apartamento.getCodigo());

        // 4. RN-02
        apartamento.validarCapacidad(totalOcupantes);

        // 5. RN-01 y RN-07
        List<Reserva> otrasActivas = ocupacion.reservas().stream()
                .filter(Reserva::esActiva)
                .filter(r -> !Objects.equals(r.getId(), idReservaExcluida))
                .toList();
        ExcepcionNegocio.exigir(otrasActivas.stream().noneMatch(r -> r.getEstancia().seSolapaCon(estancia)),
                CodigoError.NOCHES_NO_DISPONIBLES);
        ExcepcionNegocio.exigir(ocupacion.bloqueos().stream().noneMatch(b -> b.afecta(estancia.rango())),
                CodigoError.APARTAMENTO_BLOQUEADO);

        // 6. RN-20
        if (!parametros.admiteEntradaMismoDiaDeSalida()) {
            boolean contigua = otrasActivas.stream().anyMatch(r ->
                    r.getEstancia().fechaSalida().equals(estancia.fechaEntrada())
                            || r.getEstancia().fechaEntrada().equals(estancia.fechaSalida()));
            ExcepcionNegocio.exigir(!contigua, CodigoError.TIEMPO_PREPARACION_INSUFICIENTE);
        }

        // RP-01: estancia mínima de la temporada más exigente que toque la estancia
        int minimoExigido = estancia.listaNoches().stream()
                .map(contexto.calendario()::temporadaDe)
                .mapToInt(Temporada::estanciaMinimaNoches)
                .max()
                .orElse(1);
        ExcepcionNegocio.exigir(estancia.noches() >= minimoExigido, CodigoError.ESTANCIA_MINIMA_NO_CUMPLIDA,
                "mínimo " + minimoExigido + " noches");

        // RP-03: mascotas
        if (mascotas > 0) {
            ExcepcionNegocio.exigir(apartamento.admiteMascotas(), CodigoError.MASCOTAS_NO_PERMITIDAS,
                    apartamento.getCodigo());
            ExcepcionNegocio.exigir(mascotas <= parametros.maxMascotas(), CodigoError.MASCOTAS_EXCEDIDAS,
                    "máximo " + parametros.maxMascotas());
        }
    }

    /** Variante de consulta para la búsqueda: indica si el apartamento se puede vender. */
    public boolean estaDisponible(OcupacionApartamento ocupacion, Estancia estancia, int totalOcupantes,
                                  ContextoReserva contexto, LocalDate hoy) {
        try {
            verificar(ocupacion, estancia, totalOcupantes, 0, contexto, hoy, null);
            return true;
        } catch (ExcepcionNegocio e) {
            return false;
        }
    }
}
