package co.edu.uniquindio.sga.application.apartamento;

import co.edu.uniquindio.sga.domain.apartamento.SugeridorGravedad;
import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Pide al modelo de IA una sugerencia de gravedad para una novedad. Es solo una ayuda:
 * registrar la novedad nunca depende de ella (10.2).
 */
@Service
public class SugerenciaGravedadServicio {

    private static final Logger LOG = LoggerFactory.getLogger(SugerenciaGravedadServicio.class);
    private static final int LONGITUD_MAXIMA = 2_000;

    private final SugeridorGravedad sugeridor;

    public SugerenciaGravedadServicio(SugeridorGravedad sugeridor) {
        this.sugeridor = sugeridor;
    }

    /** @return la sugerencia, o vacío si la IA está deshabilitada, falló o no respondió a tiempo */
    public Optional<SugeridorGravedad.SugerenciaGravedad> sugerir(String descripcion) {
        ExcepcionNegocio.exigir(descripcion != null && !descripcion.isBlank(), CodigoError.NOVEDAD_INVALIDA,
                "describa la novedad para pedir una sugerencia");
        String texto = descripcion.length() > LONGITUD_MAXIMA ? descripcion.substring(0, LONGITUD_MAXIMA) : descripcion;
        try {
            return sugeridor.sugerir(texto.trim());
        } catch (RuntimeException e) {
            LOG.warn("La sugerencia de gravedad no está disponible: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
