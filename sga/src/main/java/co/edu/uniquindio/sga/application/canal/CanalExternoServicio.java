package co.edu.uniquindio.sga.application.canal;

import co.edu.uniquindio.sga.application.comun.AvisosReserva;
import co.edu.uniquindio.sga.application.comun.ContextoVigente;
import co.edu.uniquindio.sga.application.reserva.DisponibilidadServicio;
import co.edu.uniquindio.sga.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga.domain.apartamento.ApartamentoRepositorio;
import co.edu.uniquindio.sga.domain.apartamento.BloqueoRepositorio;
import co.edu.uniquindio.sga.domain.canal.*;
import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import co.edu.uniquindio.sga.domain.comun.Reloj;
import co.edu.uniquindio.sga.domain.reserva.*;
import co.edu.uniquindio.sga.domain.usuario.CifradorContrasenas;
import co.edu.uniquindio.sga.domain.usuario.UsuarioRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Operaciones que un canal externo ejecuta sobre el SGA (contrato 10.3.1): consultar
 * disponibilidad, crear y cancelar reservas. Todo lo intercambiado queda en la bitácora,
 * incluidos los intentos rechazados.
 */
@Service
@Transactional
public class CanalExternoServicio {

    /** Reserva enviada por el canal, identificada por su propio identificador. */
    public record SolicitudExterna(String idExterno, String codigoApartamento, LocalDate entrada, LocalDate salida,
                                   LocalTime horaEstimadaLlegada, Titular titular, List<Ocupante> acompanantes,
                                   int mascotas) {
    }

    /** @param repetida la reserva ya estaba cancelada: el mensaje se había recibido antes */
    public record CancelacionExterna(Reserva reserva, boolean repetida) {
    }

    private final CanalVentaRepositorio canales;
    private final BitacoraCanalRepositorio bitacora;
    private final ConflictoCanalRepositorio conflictos;
    private final ReservaRepositorio reservas;
    private final ApartamentoRepositorio apartamentos;
    private final BloqueoRepositorio bloqueos;
    private final UsuarioRepositorio usuarios;
    private final ContextoVigente contextoVigente;
    private final ProcesadorReservasExternas procesador;
    private final DisponibilidadServicio disponibilidad;
    private final CifradorContrasenas cifrador;
    private final AvisosReserva avisos;
    private final Reloj reloj;

    public CanalExternoServicio(CanalVentaRepositorio canales, BitacoraCanalRepositorio bitacora,
                                ConflictoCanalRepositorio conflictos, ReservaRepositorio reservas,
                                ApartamentoRepositorio apartamentos, BloqueoRepositorio bloqueos,
                                UsuarioRepositorio usuarios, ContextoVigente contextoVigente,
                                ProcesadorReservasExternas procesador, DisponibilidadServicio disponibilidad,
                                CifradorContrasenas cifrador, AvisosReserva avisos, Reloj reloj) {
        this.canales = canales;
        this.bitacora = bitacora;
        this.conflictos = conflictos;
        this.reservas = reservas;
        this.apartamentos = apartamentos;
        this.bloqueos = bloqueos;
        this.usuarios = usuarios;
        this.contextoVigente = contextoVigente;
        this.procesador = procesador;
        this.disponibilidad = disponibilidad;
        this.cifrador = cifrador;
        this.avisos = avisos;
        this.reloj = reloj;
    }

    /** Verifica la credencial del canal. Los intentos fallidos también quedan en la bitácora. */
    public CanalVenta autenticar(String codigoCanal, String credencial) {
        try {
            CanalVenta canal = canales.buscar(codigoCanal == null ? "" : codigoCanal.trim())
                    .orElseThrow(() -> new ExcepcionNegocio(CodigoError.CREDENCIAL_CANAL_INVALIDA));
            canal.autenticar(credencial, cifrador);
            return canal;
        } catch (ExcepcionNegocio e) {
            registrar(codigoCanal, TipoEventoCanal.CREDENCIAL_INVALIDA, null, null, e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<DisponibilidadServicio.Resultado> consultarDisponibilidad(CanalVenta canal, LocalDate entrada,
                                                                         LocalDate salida,
                                                                         List<LocalDate> fechasNacimiento,
                                                                         Integer personas) {
        List<DisponibilidadServicio.Resultado> resultado = disponibilidad.disponibles(new DisponibilidadServicio.Criterios(
                entrada, salida, fechasNacimiento, personas, null, null, null, null, 0));
        registrar(canal.getCodigo(), TipoEventoCanal.CONSULTA_DISPONIBILIDAD, null, null,
                "Del " + entrada + " al " + salida + ": " + resultado.size() + " apartamento(s) disponible(s)");
        return resultado;
    }

    /**
     * RN-19: si el mismo (canal, idExterno) ya se recibió, devuelve la reserva existente.
     * RN-18: si colisiona con el inventario vendido o bloqueado, se rechaza y se registra el conflicto.
     */
    public ResultadoReservaExterna crearReserva(CanalVenta canal, SolicitudExterna s) {
        String idExterno = s.idExterno();
        try {
            ReferenciaExterna referencia = new ReferenciaExterna(canal.getCodigo(), idExterno);
            Optional<Reserva> previa = reservas.buscarPorReferenciaExterna(referencia);
            if (previa.isEmpty()) {
                // El bloqueo del apartamento serializa los mensajes simultáneos; se vuelve a consultar después.
                Apartamento apartamento = apartamentos.buscarParaActualizar(s.codigoApartamento())
                        .filter(a -> !a.isEliminado())
                        .orElseThrow(() -> new ExcepcionNegocio(CodigoError.APARTAMENTO_NO_ENCONTRADO,
                                s.codigoApartamento()));
                previa = reservas.buscarPorReferenciaExterna(referencia);
                return procesar(canal, s, referencia, previa, apartamento);
            }
            return procesar(canal, s, referencia, previa, null);
        } catch (ExcepcionNegocio e) {
            registrar(canal.getCodigo(), TipoEventoCanal.RESERVA_RECHAZADA, idExterno, null, e.getMessage());
            throw e;
        }
    }

    private ResultadoReservaExterna procesar(CanalVenta canal, SolicitudExterna s, ReferenciaExterna referencia,
                                             Optional<Reserva> previa, Apartamento apartamento) {
        if (previa.isPresent()) {
            Reserva existente = previa.get();
            registrar(canal.getCodigo(), TipoEventoCanal.RESERVA_DUPLICADA, referencia.idExterno(), existente.getId(),
                    "Mensaje repetido: se devolvió la reserva existente sin crear otra (estado " + existente.getEstado() + ")");
            return new ResultadoReservaExterna.Duplicada(existente);
        }
        SolicitudReserva solicitud = new SolicitudReserva(conCuenta(s.titular()), s.acompanantes(),
                new Estancia(s.entrada(), s.salida()), s.horaEstimadaLlegada(), Canal.EXTERNO, referencia, s.mascotas());
        OcupacionApartamento ocupacion = new OcupacionApartamento(apartamento,
                reservas.activasDe(apartamento.getCodigo()), bloqueos.vigentesDe(apartamento.getCodigo()));
        ResultadoReservaExterna resultado = procesador.procesar(solicitud, previa, ocupacion,
                contextoVigente.contextoReserva(), reloj.ahora());
        switch (resultado) {
            case ResultadoReservaExterna.Aceptada(Reserva reserva) -> {
                reservas.guardar(reserva);
                registrar(canal.getCodigo(), TipoEventoCanal.RESERVA_ACEPTADA, referencia.idExterno(), reserva.getId(),
                        "Reserva creada en " + reserva.getCodigoApartamento() + " del " + s.entrada() + " al "
                                + s.salida() + " por " + reserva.getCotizacion().valorTotal());
                avisos.enviar(reserva, "Reserva recibida desde " + canal.getNombre(),
                        "Recibimos su reserva del " + s.entrada() + " al " + s.salida() + " en el apartamento "
                                + reserva.getCodigoApartamento() + ". Valor: " + reserva.getCotizacion().valorTotal() + ".");
            }
            case ResultadoReservaExterna.RechazadaPorConflicto(ConflictoCanal conflicto) -> {
                conflictos.guardar(conflicto);
                registrar(canal.getCodigo(), TipoEventoCanal.RESERVA_EN_CONFLICTO, referencia.idExterno(), null,
                        "Rechazada y registrada como conflicto: " + conflicto.motivo().mensaje());
            }
            case ResultadoReservaExterna.Duplicada ignorada -> {
                // Ya se atendió antes de procesar.
            }
        }
        return resultado;
    }

    /** Cancela una reserva del canal. Repetir el mensaje no falla: responde con la reserva ya cancelada. */
    public CancelacionExterna cancelarReserva(CanalVenta canal, String idExterno) {
        try {
            Reserva reserva = reservas.buscarPorReferenciaExterna(new ReferenciaExterna(canal.getCodigo(), idExterno))
                    .orElseThrow(() -> new ExcepcionNegocio(CodigoError.RESERVA_NO_ENCONTRADA, idExterno));
            if (reserva.getEstado() == EstadoReserva.CANCELADA) {
                registrar(canal.getCodigo(), TipoEventoCanal.CANCELACION_REPETIDA, idExterno, reserva.getId(),
                        "La reserva ya estaba cancelada");
                return new CancelacionExterna(reserva, true);
            }
            var retencion = reserva.cancelar(reloj.ahora());
            reservas.guardar(reserva);
            registrar(canal.getCodigo(), TipoEventoCanal.CANCELACION_ACEPTADA, idExterno, reserva.getId(),
                    "Cancelada. Retención según la política versión " + reserva.getPolitica().version() + ": " + retencion);
            avisos.enviar(reserva, "Reserva cancelada", "Su reserva del " + reserva.getEstancia().fechaEntrada()
                    + " fue cancelada por " + canal.getNombre() + ". Retención aplicada: " + retencion + ".");
            return new CancelacionExterna(reserva, false);
        } catch (ExcepcionNegocio e) {
            registrar(canal.getCodigo(), TipoEventoCanal.CANCELACION_RECHAZADA, idExterno, null, e.getMessage());
            throw e;
        }
    }

    /** Si el correo del titular corresponde a un huésped registrado, la reserva aparece en su cuenta. */
    private Titular conCuenta(Titular titular) {
        if (titular == null || titular.idCuenta() != null || titular.correo() == null) {
            return titular;
        }
        return usuarios.buscarPorCorreo(titular.correo())
                .map(u -> new Titular(titular.nombre(), titular.documento(), titular.correo(), titular.telefono(),
                        titular.fechaNacimiento(), u.getId()))
                .orElse(titular);
    }

    private void registrar(String canal, TipoEventoCanal tipo, String idExterno, String idReserva, String detalle) {
        bitacora.registrar(EventoCanal.de(canal, tipo, idExterno, idReserva, detalle, reloj.ahora()));
    }
}
