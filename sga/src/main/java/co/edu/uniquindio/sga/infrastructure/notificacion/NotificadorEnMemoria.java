package co.edu.uniquindio.sga.infrastructure.notificacion;

import co.edu.uniquindio.sga.domain.comun.Notificador;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación local del puerto {@link Notificador}: registra las notificaciones en el
 * log y las conserva en memoria. Permite operar y probar sin conexión ni credenciales.
 * Es el proveedor por defecto ({@code sga.notificaciones.proveedor=memoria}); la
 * alternativa real es {@link NotificadorCorreo}.
 */
@Component
@ConditionalOnProperty(name = "sga.notificaciones.proveedor", havingValue = "memoria", matchIfMissing = true)
public class NotificadorEnMemoria implements Notificador {

    private static final Logger LOG = LoggerFactory.getLogger(NotificadorEnMemoria.class);
    private static final int MAXIMO = 200;

    private final List<Notificacion> enviadas = new ArrayList<>();

    @Override
    public synchronized void notificar(Notificacion notificacion) {
        LOG.info("Notificación para {} - {}: {}", notificacion.destinatario(), notificacion.asunto(),
                notificacion.mensaje());
        enviadas.add(notificacion);
        if (enviadas.size() > MAXIMO) {
            enviadas.removeFirst();
        }
    }

    public synchronized List<Notificacion> enviadas() {
        return List.copyOf(enviadas);
    }

    public synchronized Optional<Notificacion> ultimaPara(String destinatario) {
        return enviadas.reversed().stream().filter(n -> n.destinatario().equalsIgnoreCase(destinatario)).findFirst();
    }
}
