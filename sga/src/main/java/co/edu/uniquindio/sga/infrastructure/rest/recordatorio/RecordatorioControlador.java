package co.edu.uniquindio.sga.infrastructure.rest.recordatorio;

import co.edu.uniquindio.sga.application.recordatorio.RecordatorioServicio;
import co.edu.uniquindio.sga.domain.recordatorio.Recordatorio;
import co.edu.uniquindio.sga.domain.recordatorio.TipoRecordatorio;
import co.edu.uniquindio.sga.infrastructure.rest.comun.PaginaRespuesta;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/** Recordatorios automáticos enviados y envío manual de los que corresponden hoy. */
@RestController
@RequestMapping("/api/recordatorios")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
public class RecordatorioControlador {

    public record RecordatorioR(String id, String idReserva, TipoRecordatorio tipo, String destinatario, String asunto,
                                LocalDateTime enviadoEn) {
        static RecordatorioR de(Recordatorio r) {
            return new RecordatorioR(r.id(), r.idReserva(), r.tipo(), r.destinatario(), r.asunto(), r.enviadoEn());
        }
    }

    private final RecordatorioServicio recordatorios;

    public RecordatorioControlador(RecordatorioServicio recordatorios) {
        this.recordatorios = recordatorios;
    }

    @GetMapping
    public PaginaRespuesta<RecordatorioR> listar(@RequestParam(defaultValue = "0") int pagina) {
        return PaginaRespuesta.de(recordatorios.listar(pagina), RecordatorioR::de);
    }

    /** Ejecuta ya el proceso diario. No duplica: cada reserva recibe cada recordatorio una sola vez. */
    @PostMapping("/envios")
    public List<RecordatorioR> enviarAhora() {
        return recordatorios.enviarPendientes().stream().map(RecordatorioR::de).toList();
    }
}
