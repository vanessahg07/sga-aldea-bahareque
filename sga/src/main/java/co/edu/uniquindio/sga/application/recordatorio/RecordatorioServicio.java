package co.edu.uniquindio.sga.application.recordatorio;

import co.edu.uniquindio.sga.application.comun.AvisosReserva;
import co.edu.uniquindio.sga.application.comun.ContextoVigente;
import co.edu.uniquindio.sga.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga.domain.comun.Pagina;
import co.edu.uniquindio.sga.domain.comun.Reloj;
import co.edu.uniquindio.sga.domain.recordatorio.PlanificadorRecordatorios;
import co.edu.uniquindio.sga.domain.recordatorio.Recordatorio;
import co.edu.uniquindio.sga.domain.recordatorio.RecordatorioRepositorio;
import co.edu.uniquindio.sga.domain.reserva.ReservaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Recordatorios automáticos antes de la llegada y antes de la salida (funcionalidad opcional 6). */
@Service
@Transactional
public class RecordatorioServicio {

    private final ReservaRepositorio reservas;
    private final RecordatorioRepositorio recordatorios;
    private final ContextoVigente contextoVigente;
    private final PlanificadorRecordatorios planificador;
    private final AvisosReserva avisos;
    private final Reloj reloj;

    public RecordatorioServicio(ReservaRepositorio reservas, RecordatorioRepositorio recordatorios,
                                ContextoVigente contextoVigente, PlanificadorRecordatorios planificador,
                                AvisosReserva avisos, Reloj reloj) {
        this.reservas = reservas;
        this.recordatorios = recordatorios;
        this.contextoVigente = contextoVigente;
        this.planificador = planificador;
        this.avisos = avisos;
        this.reloj = reloj;
    }

    /**
     * Envía los recordatorios que corresponden hoy. Solo se registra como enviado el que el
     * notificador aceptó; si falla, se intentará de nuevo en la siguiente ejecución.
     *
     * @return recordatorios enviados
     */
    public List<Recordatorio> enviarPendientes() {
        Alojamiento alojamiento = contextoVigente.alojamiento();
        LocalDate hoy = reloj.hoy();
        int dias = Math.max(alojamiento.getParametros().diasRecordatorioLlegada(), 1);
        List<PlanificadorRecordatorios.Pendiente> pendientes = planificador.pendientes(
                reservas.activasEnRango(hoy, hoy.plusDays(dias + 1L)), hoy, alojamiento.getParametros(),
                alojamiento.getNombre(), recordatorios::fueEnviado);
        return pendientes.stream()
                .filter(p -> avisos.enviar(p.destinatario(), p.asunto(), p.mensaje()))
                .map(p -> {
                    Recordatorio enviado = Recordatorio.de(p.reserva().getId(), p.tipo(), p.destinatario(), p.asunto(),
                            reloj.ahora());
                    recordatorios.guardar(enviado);
                    return enviado;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Pagina<Recordatorio> listar(int pagina) {
        return recordatorios.listar(pagina);
    }
}
