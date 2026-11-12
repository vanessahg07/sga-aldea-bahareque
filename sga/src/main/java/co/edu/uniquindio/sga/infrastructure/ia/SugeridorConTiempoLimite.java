package co.edu.uniquindio.sga.infrastructure.ia;

import co.edu.uniquindio.sga.domain.apartamento.SugeridorGravedad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.*;

/**
 * Garantiza el tiempo máximo de espera de la IA (I-03) sin importar el proveedor: si no
 * responde a tiempo, la sugerencia se descarta y el usuario sigue trabajando.
 */
public class SugeridorConTiempoLimite implements SugeridorGravedad {

    private static final Logger LOG = LoggerFactory.getLogger(SugeridorConTiempoLimite.class);

    private final SugeridorGravedad modelo;
    private final long timeoutMs;
    private final ExecutorService ejecutor = Executors.newVirtualThreadPerTaskExecutor();

    public SugeridorConTiempoLimite(SugeridorGravedad modelo, long timeoutMs) {
        this.modelo = modelo;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public Optional<SugerenciaGravedad> sugerir(String descripcion) {
        Future<Optional<SugerenciaGravedad>> tarea = ejecutor.submit(() -> modelo.sugerir(descripcion));
        try {
            return tarea.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            tarea.cancel(true);
            LOG.warn("La IA no respondió en {} ms; se continúa sin sugerencia", timeoutMs);
            return Optional.empty();
        } catch (ExecutionException e) {
            LOG.warn("La IA falló; se continúa sin sugerencia: {}", e.getCause().getMessage());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }
}
