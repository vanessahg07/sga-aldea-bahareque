package co.edu.uniquindio.sga.infrastructure.configuracion;

import co.edu.uniquindio.sga.application.recordatorio.RecordatorioServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Envía cada día los recordatorios de llegada y de salida (funcionalidad opcional 6). */
@Component
public class TareaRecordatorios {

    private static final Logger LOG = LoggerFactory.getLogger(TareaRecordatorios.class);

    private final RecordatorioServicio recordatorios;

    public TareaRecordatorios(RecordatorioServicio recordatorios) {
        this.recordatorios = recordatorios;
    }

    @Scheduled(cron = "${sga.recordatorios.cron:0 0 8 * * *}", zone = "${sga.zona-horaria:America/Bogota}")
    public void enviar() {
        try {
            int enviados = recordatorios.enviarPendientes().size();
            LOG.info("Recordatorios enviados hoy: {}", enviados);
        } catch (RuntimeException e) {
            LOG.error("Falló el envío de recordatorios", e);
        }
    }
}
