package co.edu.uniquindio.sga.application.reserva;

import co.edu.uniquindio.sga.application.comun.AvisosReserva;
import co.edu.uniquindio.sga.application.comun.ContextoVigente;
import co.edu.uniquindio.sga.application.comun.Solicitante;
import co.edu.uniquindio.sga.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga.domain.apartamento.ApartamentoRepositorio;
import co.edu.uniquindio.sga.domain.apartamento.BloqueoRepositorio;
import co.edu.uniquindio.sga.domain.comun.*;
import co.edu.uniquindio.sga.domain.folio.AutorizacionCierre;
import co.edu.uniquindio.sga.domain.reserva.*;
import co.edu.uniquindio.sga.domain.usuario.Usuario;
import co.edu.uniquindio.sga.domain.usuario.UsuarioRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Casos de uso del ciclo de vida de la reserva. */
@Service
@Transactional
public class ReservaServicio {

    /** Datos comunes para crear o modificar una reserva. */
    public record DatosEstancia(String codigoApartamento, LocalDate entrada, LocalDate salida,
                                List<Ocupante> acompanantes, LocalTime horaEstimadaLlegada, int mascotas) {
    }

    private final ReservaRepositorio reservas;
    private final ApartamentoRepositorio apartamentos;
    private final BloqueoRepositorio bloqueos;
    private final UsuarioRepositorio usuarios;
    private final ContextoVigente contextoVigente;
    private final GestorReservas gestor;
    private final AvisosReserva avisos;
    private final Reloj reloj;

    public ReservaServicio(ReservaRepositorio reservas, ApartamentoRepositorio apartamentos, BloqueoRepositorio bloqueos,
                           UsuarioRepositorio usuarios, ContextoVigente contextoVigente, GestorReservas gestor,
                           AvisosReserva avisos, Reloj reloj) {
        this.reservas = reservas;
        this.apartamentos = apartamentos;
        this.bloqueos = bloqueos;
        this.usuarios = usuarios;
        this.contextoVigente = contextoVigente;
        this.gestor = gestor;
        this.avisos = avisos;
        this.reloj = reloj;
    }

    // --- Creación ---

    /** Canal PORTAL: el titular es el huésped que reserva, con los datos de su cuenta. */
    public Reserva crearPorPortal(DatosEstancia datos, Solicitante huesped) {
        Usuario usuario = usuarios.buscar(huesped.idUsuario())
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.USUARIO_NO_ENCONTRADO));
        Titular titular = new Titular(usuario.getNombre(), usuario.getDocumento(), usuario.getCorreo(),
                usuario.getTelefono(), usuario.getFechaNacimiento(), usuario.getId());
        return crear(titular, datos, Canal.PORTAL);
    }

    /** Canal DIRECTO: la recepción registra la reserva a nombre de un titular que puede no tener cuenta. */
    public Reserva crearDirecta(DatosEstancia datos, Titular titular) {
        Titular conCuenta = titular.idCuenta() != null ? titular : usuarios.buscarPorCorreo(titular.correo())
                .map(u -> new Titular(titular.nombre(), titular.documento(), titular.correo(), titular.telefono(),
                        titular.fechaNacimiento(), u.getId()))
                .orElse(titular);
        return crear(conCuenta, datos, Canal.DIRECTO);
    }

    private Reserva crear(Titular titular, DatosEstancia datos, Canal canal) {
        Apartamento apartamento = bloquearApartamento(datos.codigoApartamento());
        SolicitudReserva solicitud = new SolicitudReserva(titular, datos.acompanantes(),
                new Estancia(datos.entrada(), datos.salida()), datos.horaEstimadaLlegada(), canal, null, datos.mascotas());
        Reserva reserva = gestor.crear(solicitud, ocupacion(apartamento), contextoVigente.contextoReserva(), reloj.ahora());
        reservas.guardar(reserva);
        notificar(reserva, "Reserva recibida",
                "Recibimos su reserva del " + reserva.getEstancia().fechaEntrada() + " al "
                        + reserva.getEstancia().fechaSalida() + " en el apartamento " + reserva.getCodigoApartamento()
                        + ". Valor: " + reserva.getCotizacion().valorTotal()
                        + ". Queda pendiente de confirmación con el pago del anticipo.");
        return reserva;
    }

    // --- Modificación ---

    /** RN-14: cambia fechas, grupo o apartamento de una reserva no iniciada y registra el ajuste. */
    public Reserva modificar(String id, DatosEstancia datos) {
        Reserva reserva = buscar(id);
        String codigoDestino = datos.codigoApartamento() == null ? reserva.getCodigoApartamento() : datos.codigoApartamento();
        Apartamento destino = bloquearApartamento(codigoDestino);
        gestor.modificar(reserva, new Estancia(datos.entrada(), datos.salida()), datos.acompanantes(), datos.mascotas(),
                ocupacion(destino), contextoVigente.contextoReserva(), reloj.ahora());
        if (datos.horaEstimadaLlegada() != null) {
            reserva.registrarHoraEstimadaLlegada(datos.horaEstimadaLlegada());
        }
        reservas.guardar(reserva);
        return reserva;
    }

    public Reserva registrarHoraLlegada(String id, LocalTime hora, Solicitante solicitante) {
        Reserva reserva = buscar(id);
        solicitante.exigirAcceso(reserva);
        reserva.registrarHoraEstimadaLlegada(hora);
        reservas.guardar(reserva);
        return reserva;
    }

    // --- Transiciones ---

    public Reserva confirmar(String id) {
        Reserva reserva = buscar(id);
        reserva.confirmar(parametros());
        reservas.guardar(reserva);
        notificar(reserva, "Reserva confirmada", "Su reserva del " + reserva.getEstancia().fechaEntrada()
                + " en el apartamento " + reserva.getCodigoApartamento() + " está confirmada.");
        return reserva;
    }

    /** El huésped solo cancela sus propias reservas. La retención usa la política congelada. */
    public Reserva cancelar(String id, Solicitante solicitante) {
        Reserva reserva = buscar(id);
        solicitante.exigirAcceso(reserva);
        Dinero retencion = reserva.cancelar(reloj.ahora());
        reservas.guardar(reserva);
        notificar(reserva, "Reserva cancelada", "Su reserva del " + reserva.getEstancia().fechaEntrada()
                + " fue cancelada. Retención aplicada: " + retencion + ". Saldo del folio: " + reserva.getFolio().saldo() + ".");
        return reserva;
    }

    public Reserva declararNoShow(String id) {
        Reserva reserva = buscar(id);
        reserva.declararNoShow(reloj.ahora(), parametros());
        reservas.guardar(reserva);
        return reserva;
    }

    public Reserva registrarLlegada(String id) {
        Reserva reserva = buscar(id);
        Apartamento apartamento = bloquearApartamento(reserva.getCodigoApartamento());
        reserva.registrarLlegada(reloj.hoy(), apartamento);
        apartamentos.guardar(apartamento);
        reservas.guardar(reserva);
        return reserva;
    }

    /**
     * Registra la salida. Si el folio tiene saldo, solo el administrador puede autorizar
     * el cierre indicando el motivo; la autorización queda registrada con su autor.
     */
    public Reserva registrarSalida(String id, String motivoAutorizacion, Solicitante solicitante) {
        Reserva reserva = buscar(id);
        AutorizacionCierre autorizacion = null;
        if (motivoAutorizacion != null && !motivoAutorizacion.isBlank()) {
            solicitante.exigirAdministrador();
            autorizacion = new AutorizacionCierre(solicitante.nombre(), motivoAutorizacion, reloj.ahora());
        }
        Apartamento apartamento = bloquearApartamento(reserva.getCodigoApartamento());
        reserva.registrarSalida(apartamento, autorizacion);
        apartamentos.guardar(apartamento);
        reservas.guardar(reserva);
        return reserva;
    }

    /** RN-21: cancela las reservas pendientes que superaron el plazo de confirmación. */
    public int vencerPendientes() {
        ParametrosAlojamiento parametros = parametros();
        int vencidas = 0;
        for (Reserva reserva : reservas.pendientes()) {
            if (reserva.vencerSiCorresponde(reloj.ahora(), parametros)) {
                reservas.guardar(reserva);
                notificar(reserva, "Reserva vencida", "Su reserva del " + reserva.getEstancia().fechaEntrada()
                        + " se canceló porque no se confirmó dentro del plazo.");
                vencidas++;
            }
        }
        return vencidas;
    }

    // --- Consultas ---

    @Transactional(readOnly = true)
    public Reserva consultar(String id, Solicitante solicitante) {
        Reserva reserva = buscar(id);
        solicitante.exigirAcceso(reserva);
        return reserva;
    }

    @Transactional(readOnly = true)
    public Pagina<Reserva> listar(FiltroReservas filtro, int pagina) {
        return reservas.listar(filtro, pagina);
    }

    @Transactional(readOnly = true)
    public Pagina<Reserva> mias(Solicitante huesped, int pagina) {
        return reservas.listar(FiltroReservas.deCuenta(huesped.idUsuario()), pagina);
    }

    /** Llegadas previstas: reservas confirmadas o pendientes con entrada en la fecha. */
    @Transactional(readOnly = true)
    public List<Reserva> llegadas(LocalDate fecha) {
        return reservas.conEntradaEl(fecha).stream()
                .filter(r -> r.getEstado() == EstadoReserva.CONFIRMADA || r.getEstado() == EstadoReserva.PENDIENTE)
                .toList();
    }

    /** Salidas previstas: reservas en curso con salida en la fecha. */
    @Transactional(readOnly = true)
    public List<Reserva> salidas(LocalDate fecha) {
        return reservas.conSalidaEl(fecha).stream()
                .filter(r -> r.getEstado() == EstadoReserva.EN_CURSO)
                .toList();
    }

    /** Calendario de ocupación: reservas activas que tocan el rango. */
    @Transactional(readOnly = true)
    public List<Reserva> ocupacion(LocalDate desde, LocalDate hasta) {
        RangoFechas rango = new RangoFechas(desde, hasta);
        return reservas.activasEnRango(rango.inicio(), rango.fin());
    }

    // --- Apoyo ---

    Reserva buscar(String id) {
        return reservas.buscar(id).orElseThrow(() -> new ExcepcionNegocio(CodigoError.RESERVA_NO_ENCONTRADA, id));
    }

    private Apartamento bloquearApartamento(String codigo) {
        return apartamentos.buscarParaActualizar(codigo)
                .filter(a -> !a.isEliminado())
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.APARTAMENTO_NO_ENCONTRADO, codigo));
    }

    private OcupacionApartamento ocupacion(Apartamento apartamento) {
        return new OcupacionApartamento(apartamento, reservas.activasDe(apartamento.getCodigo()),
                bloqueos.vigentesDe(apartamento.getCodigo()));
    }

    private ParametrosAlojamiento parametros() {
        return contextoVigente.alojamiento().getParametros();
    }

    private void notificar(Reserva reserva, String asunto, String mensaje) {
        avisos.enviar(reserva, asunto, mensaje);
    }
}
