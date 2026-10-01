package co.edu.uniquindio.sga.domain.reserva;

import co.edu.uniquindio.sga.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.Dinero;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import co.edu.uniquindio.sga.domain.folio.AutorizacionCierre;
import co.edu.uniquindio.sga.domain.folio.Folio;
import co.edu.uniquindio.sga.domain.folio.TipoCargo;
import co.edu.uniquindio.sga.domain.politica.PoliticaCancelacion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static co.edu.uniquindio.sga.domain.reserva.EstadoReserva.*;

/**
 * Compromiso de ocupar un apartamento durante una estancia, para un conjunto definido
 * de ocupantes. Congela su valor y la versión de la política de cancelación al crearse
 * (RN-22) y controla su ciclo de vida (RN-08).
 * <p>
 * Se crea a través de {@link GestorReservas}, que verifica la disponibilidad antes.
 */
public class Reserva {

    private final String id;
    private final Titular titular;
    private List<Ocupante> acompanantes;
    private String codigoApartamento;
    private Estancia estancia;
    private LocalTime horaEstimadaLlegada;
    private final Canal canal;
    private final ReferenciaExterna referenciaExterna;
    private int mascotas;
    private EstadoReserva estado;
    private final LocalDateTime creadaEn;
    private CotizacionEstancia cotizacion;
    private final PoliticaCancelacion politica;
    private final Folio folio;

    private Reserva(SolicitudReserva solicitud, String codigoApartamento, CotizacionEstancia cotizacion,
                    PoliticaCancelacion politica, LocalDateTime creadaEn) {
        this(UUID.randomUUID().toString(), solicitud, codigoApartamento, cotizacion, politica, creadaEn, null);
    }

    private Reserva(String id, SolicitudReserva solicitud, String codigoApartamento, CotizacionEstancia cotizacion,
                    PoliticaCancelacion politica, LocalDateTime creadaEn, Folio folio) {
        ExcepcionNegocio.exigir(solicitud.titular() != null, CodigoError.TITULAR_INVALIDO);
        ExcepcionNegocio.exigir(solicitud.canal() != null, CodigoError.RESERVA_INVALIDA, "canal");
        ExcepcionNegocio.exigir(solicitud.mascotas() >= 0, CodigoError.RESERVA_INVALIDA, "mascotas");
        ExcepcionNegocio.exigir((solicitud.canal() == Canal.EXTERNO) == (solicitud.referenciaExterna() != null),
                CodigoError.RESERVA_EXTERNA_INVALIDA);
        this.id = id;
        this.titular = solicitud.titular();
        this.acompanantes = solicitud.acompanantes();
        this.codigoApartamento = codigoApartamento;
        this.estancia = solicitud.estancia();
        this.horaEstimadaLlegada = solicitud.horaEstimadaLlegada();
        this.canal = solicitud.canal();
        this.referenciaExterna = solicitud.referenciaExterna();
        this.mascotas = solicitud.mascotas();
        this.estado = PENDIENTE;
        this.creadaEn = creadaEn;
        this.cotizacion = cotizacion;
        this.politica = politica;
        this.folio = folio != null ? folio : Folio.abrir(id);
    }

    /**
     * Reconstruye una reserva ya existente, tal como fue guardada. No vuelve a validar
     * disponibilidad ni recalcula nada: el valor y la política siguen congelados.
     */
    public static Reserva reconstituir(String id, SolicitudReserva datos, String codigoApartamento,
                                       EstadoReserva estado, LocalDateTime creadaEn, CotizacionEstancia cotizacion,
                                       PoliticaCancelacion politica, Folio folio) {
        Reserva reserva = new Reserva(id, datos, codigoApartamento, cotizacion, politica, creadaEn, folio);
        reserva.estado = estado;
        return reserva;
    }

    /** La reserva nace PENDIENTE, con su valor y su política congelados y el folio abierto. */
    static Reserva crear(SolicitudReserva solicitud, String codigoApartamento, CotizacionEstancia cotizacion,
                         PoliticaCancelacion politicaVigente, LocalDateTime ahora) {
        Reserva reserva = new Reserva(solicitud, codigoApartamento, cotizacion, politicaVigente, ahora);
        reserva.folio.agregarCargo(TipoCargo.ALOJAMIENTO, cotizacion.valorAlojamiento(),
                "Alojamiento " + solicitud.estancia().noches() + " noches", ahora);
        if (cotizacion.valorMascotas().esPositivo()) {
            reserva.folio.agregarCargo(TipoCargo.MASCOTA, cotizacion.valorMascotas(),
                    "Mascotas: " + solicitud.mascotas(), ahora);
        }
        return reserva;
    }

    // --- Ciclo de vida ---

    public void registrarHoraEstimadaLlegada(LocalTime hora) {
        ExcepcionNegocio.exigir(hora != null, CodigoError.HORA_LLEGADA_REQUERIDA);
        ExcepcionNegocio.exigir(estado.esActiva(), CodigoError.RESERVA_NO_MODIFICABLE);
        this.horaEstimadaLlegada = hora;
    }

    /** PENDIENTE → CONFIRMADA. RN-09 y anticipo configurado. */
    public void confirmar(ParametrosAlojamiento parametros) {
        estado.validarTransicionA(CONFIRMADA);
        ExcepcionNegocio.exigir(horaEstimadaLlegada != null, CodigoError.HORA_LLEGADA_REQUERIDA);
        ExcepcionNegocio.exigir(!folio.totalPagos().esMenorQue(anticipoRequerido(parametros)),
                CodigoError.ANTICIPO_INSUFICIENTE, "requerido " + anticipoRequerido(parametros));
        estado = CONFIRMADA;
    }

    public Dinero anticipoRequerido(ParametrosAlojamiento parametros) {
        return cotizacion.valorTotal().porcentaje(parametros.porcentajeAnticipo()).redondear();
    }

    /**
     * PENDIENTE/CONFIRMADA → CANCELADA. La retención se calcula con la política
     * congelada (RN-13); el cargo original se revierte y la retención queda como
     * penalidad, de modo que lo pagado de más aparece como saldo a favor.
     */
    public Dinero cancelar(LocalDateTime ahora) {
        estado.validarTransicionA(CANCELADA);
        Dinero retencion = politica.retencionPorCancelacion(cotizacion.valorTotal(), ahora.toLocalDate(),
                estancia.fechaEntrada());
        terminarSinEstancia(CANCELADA, "Cancelación", retencion, ahora);
        return retencion;
    }

    /** RN-21: una reserva PENDIENTE que supera el plazo de confirmación se cancela sola, sin penalidad. */
    public boolean vencerSiCorresponde(LocalDateTime ahora, ParametrosAlojamiento parametros) {
        if (estado != PENDIENTE || ahora.isBefore(creadaEn.plusHours(parametros.horasPlazoConfirmacion()))) {
            return false;
        }
        terminarSinEstancia(CANCELADA, "Vencimiento del plazo de confirmación", Dinero.CERO, ahora);
        return true;
    }

    /** CONFIRMADA → NO_SHOW, a partir de la hora límite del día de entrada. RN-13. */
    public Dinero declararNoShow(LocalDateTime ahora, ParametrosAlojamiento parametros) {
        estado.validarTransicionA(NO_SHOW);
        LocalDateTime limite = estancia.fechaEntrada().atTime(parametros.horaLimiteNoShow());
        ExcepcionNegocio.exigir(!ahora.isBefore(limite), CodigoError.NO_SHOW_ANTICIPADO);
        Dinero retencion = politica.retencionPorNoShow(cotizacion.valorTotal());
        terminarSinEstancia(NO_SHOW, "No-show", retencion, ahora);
        return retencion;
    }

    /** CONFIRMADA → EN_CURSO. RN-10 y RN-11. */
    public void registrarLlegada(LocalDate hoy, Apartamento apartamento) {
        estado.validarTransicionA(EN_CURSO);
        ExcepcionNegocio.exigir(!hoy.isBefore(estancia.fechaEntrada()), CodigoError.REGISTRO_ANTES_DE_ENTRADA);
        ExcepcionNegocio.exigir(apartamento.getCodigo().equals(codigoApartamento), CodigoError.RESERVA_INVALIDA,
                "el apartamento no corresponde a la reserva");
        apartamento.recibirGrupo();
        estado = EN_CURSO;
    }

    /**
     * EN_CURSO → FINALIZADA. El cierre del folio es requisito; con saldo distinto de
     * cero se necesita autorización del administrador (puede ser null si no hay saldo).
     */
    public void registrarSalida(Apartamento apartamento, AutorizacionCierre autorizacion) {
        estado.validarTransicionA(FINALIZADA);
        if (!folio.isCerrado()) {
            if (folio.saldo().esCero() || autorizacion == null) {
                folio.cerrar();
            } else {
                folio.cerrarConAutorizacion(autorizacion);
            }
        }
        apartamento.liberar();
        estado = FINALIZADA;
    }

    /**
     * Aplica una modificación ya validada (RN-14). La diferencia de valor se registra
     * como ajuste; la política congelada no cambia (RN-22).
     */
    void aplicarModificacion(String nuevoCodigoApartamento, Estancia nuevaEstancia, List<Ocupante> nuevosAcompanantes,
                             int nuevasMascotas, CotizacionEstancia nuevaCotizacion, LocalDateTime ahora) {
        validarModificable();
        Dinero diferencia = nuevaCotizacion.valorTotal().menos(cotizacion.valorTotal());
        if (!diferencia.esCero()) {
            folio.agregarCargo(TipoCargo.AJUSTE, diferencia, "Ajuste por modificación de la reserva", ahora);
        }
        this.codigoApartamento = nuevoCodigoApartamento;
        this.estancia = nuevaEstancia;
        this.acompanantes = List.copyOf(nuevosAcompanantes);
        this.mascotas = nuevasMascotas;
        this.cotizacion = nuevaCotizacion;
    }

    void validarModificable() {
        ExcepcionNegocio.exigir(estado == PENDIENTE || estado == CONFIRMADA, CodigoError.RESERVA_NO_MODIFICABLE);
    }

    private void terminarSinEstancia(EstadoReserva destino, String motivo, Dinero retencion, LocalDateTime ahora) {
        folio.anularCargosVigentes(motivo, ahora);
        if (retencion.esPositivo()) {
            folio.agregarCargo(TipoCargo.PENALIDAD, retencion,
                    motivo + " - política v" + politica.version(), ahora);
        }
        estado = destino;
    }

    // --- Consultas ---

    /** RN-12: solo las reservas activas retienen noches. */
    public boolean esActiva() {
        return estado.esActiva();
    }

    /** Todos los ocupantes: el titular y sus acompañantes. */
    public List<Ocupante> ocupantes() {
        List<Ocupante> todos = new ArrayList<>();
        todos.add(titular.comoOcupante());
        todos.addAll(acompanantes);
        return List.copyOf(todos);
    }

    public String getId() { return id; }
    public Titular getTitular() { return titular; }
    public List<Ocupante> getAcompanantes() { return acompanantes; }
    public String getCodigoApartamento() { return codigoApartamento; }
    public Estancia getEstancia() { return estancia; }
    public Optional<LocalTime> getHoraEstimadaLlegada() { return Optional.ofNullable(horaEstimadaLlegada); }
    public Canal getCanal() { return canal; }
    public Optional<ReferenciaExterna> getReferenciaExterna() { return Optional.ofNullable(referenciaExterna); }
    public int getMascotas() { return mascotas; }
    public EstadoReserva getEstado() { return estado; }
    public LocalDateTime getCreadaEn() { return creadaEn; }
    public CotizacionEstancia getCotizacion() { return cotizacion; }
    public PoliticaCancelacion getPolitica() { return politica; }
    public Folio getFolio() { return folio; }

    // --- Identidad: dos reservas son la misma si tienen el mismo identificador ---

    @Override
    public boolean equals(Object otra) {
        return this == otra || otra instanceof Reserva r && id.equals(r.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
