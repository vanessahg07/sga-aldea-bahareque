package co.edu.uniquindio.sga.domain.reserva;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de dominio para crear y modificar reservas: verifica disponibilidad,
 * cotiza y congela el valor y la política.
 */
public class GestorReservas {

    private final VerificadorDisponibilidad verificador;
    private final CotizadorEstancia cotizador;

    public GestorReservas() {
        this(new VerificadorDisponibilidad(), new CotizadorEstancia());
    }

    public GestorReservas(VerificadorDisponibilidad verificador, CotizadorEstancia cotizador) {
        this.verificador = verificador;
        this.cotizador = cotizador;
    }

    public Reserva crear(SolicitudReserva solicitud, OcupacionApartamento ocupacion, ContextoReserva contexto,
                         LocalDateTime ahora) {
        ExcepcionNegocio.exigir(solicitud.titular() != null && solicitud.estancia() != null,
                CodigoError.RESERVA_INVALIDA);
        List<Ocupante> ocupantes = new ArrayList<>();
        ocupantes.add(solicitud.titular().comoOcupante());
        ocupantes.addAll(solicitud.acompanantes());

        verificador.verificar(ocupacion, solicitud.estancia(), ocupantes.size(), solicitud.mascotas(), contexto,
                ahora.toLocalDate(), null);
        ExcepcionNegocio.exigir(solicitud.titular().comoOcupante().esFacturable(solicitud.estancia().fechaEntrada(),
                contexto.parametros().umbralEdadFacturable()), CodigoError.TITULAR_NO_FACTURABLE);

        String codigoApartamento = ocupacion.apartamento().getCodigo();
        CotizacionEstancia cotizacion = cotizador.cotizar(codigoApartamento, solicitud.estancia(), ocupantes,
                solicitud.mascotas(), contexto);
        return Reserva.crear(solicitud, codigoApartamento, cotizacion, contexto.politicaVigente(), ahora);
    }

    /**
     * RN-14: toda modificación repite las validaciones de creación y recalcula el valor
     * con las tarifas vigentes. La diferencia queda como ajuste en el folio.
     *
     * @param ocupacionDestino ocupación del apartamento asignado tras el cambio (el mismo u otro)
     */
    public void modificar(Reserva reserva, Estancia nuevaEstancia, List<Ocupante> nuevosAcompanantes,
                          int nuevasMascotas, OcupacionApartamento ocupacionDestino, ContextoReserva contexto,
                          LocalDateTime ahora) {
        reserva.validarModificable();
        List<Ocupante> ocupantes = new ArrayList<>();
        ocupantes.add(reserva.getTitular().comoOcupante());
        ocupantes.addAll(nuevosAcompanantes);

        verificador.verificar(ocupacionDestino, nuevaEstancia, ocupantes.size(), nuevasMascotas, contexto,
                ahora.toLocalDate(), reserva.getId());

        String codigoDestino = ocupacionDestino.apartamento().getCodigo();
        CotizacionEstancia nuevaCotizacion = cotizador.cotizar(codigoDestino, nuevaEstancia, ocupantes,
                nuevasMascotas, contexto);
        reserva.aplicarModificacion(codigoDestino, nuevaEstancia, nuevosAcompanantes, nuevasMascotas,
                nuevaCotizacion, ahora);
    }
}
