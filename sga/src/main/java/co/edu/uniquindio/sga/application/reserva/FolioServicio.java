package co.edu.uniquindio.sga.application.reserva;

import co.edu.uniquindio.sga.application.comun.ContextoVigente;
import co.edu.uniquindio.sga.application.comun.Solicitante;
import co.edu.uniquindio.sga.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga.domain.alojamiento.ServicioAdicional;
import co.edu.uniquindio.sga.domain.comun.*;
import co.edu.uniquindio.sga.domain.folio.AutorizacionCierre;
import co.edu.uniquindio.sga.domain.folio.Folio;
import co.edu.uniquindio.sga.domain.folio.MedioPago;
import co.edu.uniquindio.sga.domain.folio.TipoCargo;
import co.edu.uniquindio.sga.domain.reserva.Reserva;
import co.edu.uniquindio.sga.domain.reserva.ReservaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Casos de uso del folio: cargos, pagos, reversos y cierre. */
@Service
@Transactional
public class FolioServicio {

    private final ReservaRepositorio reservas;
    private final ContextoVigente contextoVigente;
    private final Reloj reloj;

    public FolioServicio(ReservaRepositorio reservas, ContextoVigente contextoVigente, Reloj reloj) {
        this.reservas = reservas;
        this.contextoVigente = contextoVigente;
        this.reloj = reloj;
    }

    /** El huésped solo consulta el folio de sus propias reservas. */
    @Transactional(readOnly = true)
    public Reserva consultar(String idReserva, Solicitante solicitante) {
        Reserva reserva = buscar(idReserva);
        solicitante.exigirAcceso(reserva);
        return reserva;
    }

    /** RN-15: todo pago tiene medio (aceptado por el alojamiento) y fecha. */
    public Reserva registrarPago(String idReserva, BigDecimal valor, String medio, LocalDateTime fecha) {
        Reserva reserva = buscar(idReserva);
        MedioPago medioPago = new MedioPago(medio);
        contextoVigente.alojamiento().validarMedioPago(medioPago);
        reserva.getFolio().registrarPago(Dinero.de(valor), medioPago, fecha == null ? reloj.ahora() : fecha);
        reservas.guardar(reserva);
        return reserva;
    }

    /** Cargo por un servicio adicional configurado en el alojamiento. */
    public Reserva cargarServicio(String idReserva, String codigoServicio, int cantidad) {
        Reserva reserva = buscar(idReserva);
        Alojamiento alojamiento = contextoVigente.alojamiento();
        ServicioAdicional servicio = alojamiento.servicio(codigoServicio);
        ExcepcionNegocio.exigir(servicio.generaCargo(), CodigoError.CARGO_INVALIDO, "el servicio no genera cargo");
        reserva.getFolio().agregarCargo(TipoCargo.SERVICIO_ADICIONAL, servicio.valorPor(cantidad),
                servicio.nombre() + " x" + cantidad, reloj.ahora());
        reservas.guardar(reserva);
        return reserva;
    }

    /** Ajuste manual, positivo o negativo, con descripción obligatoria. */
    public Reserva registrarAjuste(String idReserva, BigDecimal valor, String descripcion) {
        Reserva reserva = buscar(idReserva);
        ExcepcionNegocio.exigir(valor != null && valor.signum() != 0, CodigoError.CARGO_INVALIDO, "valor");
        reserva.getFolio().agregarCargo(TipoCargo.AJUSTE, Dinero.de(valor), descripcion, reloj.ahora());
        reservas.guardar(reserva);
        return reserva;
    }

    /** RN-16: la corrección es un movimiento inverso. */
    public Reserva revertirCargo(String idReserva, String idCargo, String motivo) {
        Reserva reserva = buscar(idReserva);
        reserva.getFolio().revertirCargo(idCargo, motivo, reloj.ahora());
        reservas.guardar(reserva);
        return reserva;
    }

    public Reserva revertirPago(String idReserva, String idPago) {
        Reserva reserva = buscar(idReserva);
        reserva.getFolio().revertirPago(idPago, reloj.ahora());
        reservas.guardar(reserva);
        return reserva;
    }

    /**
     * RN-17: con saldo cero se cierra directamente; con saldo distinto de cero solo el
     * administrador puede cerrarlo, indicando el motivo.
     */
    public Reserva cerrar(String idReserva, String motivoAutorizacion, Solicitante solicitante) {
        Reserva reserva = buscar(idReserva);
        Folio folio = reserva.getFolio();
        if (folio.saldo().esCero() || motivoAutorizacion == null || motivoAutorizacion.isBlank()) {
            folio.cerrar();
        } else {
            solicitante.exigirAdministrador();
            folio.cerrarConAutorizacion(new AutorizacionCierre(solicitante.nombre(), motivoAutorizacion, reloj.ahora()));
        }
        reservas.guardar(reserva);
        return reserva;
    }

    private Reserva buscar(String id) {
        return reservas.buscar(id).orElseThrow(() -> new ExcepcionNegocio(CodigoError.RESERVA_NO_ENCONTRADA, id));
    }
}
