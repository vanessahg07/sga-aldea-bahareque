package co.edu.uniquindio.sga.infrastructure.ia;

import co.edu.uniquindio.sga.domain.apartamento.SugeridorGravedad;
import co.edu.uniquindio.sga.infrastructure.configuracion.PropiedadesSga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/**
 * Elige la implementación del modelo de IA según la configuración. Apagar la IA
 * ({@code sga.ia.habilitada=false}) o cambiar de proveedor no requiere tocar código.
 */
@Configuration
public class ConfiguracionIa {

    private static final Logger LOG = LoggerFactory.getLogger(ConfiguracionIa.class);

    /** Estado de la IA para mostrarlo en la interfaz. */
    public record EstadoIa(boolean habilitada, String proveedor) {
    }

    @Bean
    public EstadoIa estadoIa(PropiedadesSga propiedades) {
        PropiedadesSga.Ia ia = propiedades.ia();
        return new EstadoIa(ia.habilitada(), ia.habilitada() ? ia.proveedor() : "deshabilitada");
    }

    @Bean
    public SugeridorGravedad sugeridorGravedad(PropiedadesSga propiedades) {
        PropiedadesSga.Ia ia = propiedades.ia();
        if (!ia.habilitada()) {
            LOG.info("IA deshabilitada: las novedades se registran sin sugerencia de gravedad");
            return descripcion -> Optional.empty();
        }
        SugeridorGravedad modelo = switch (ia.proveedor()) {
            case "openai" -> new SugeridorGravedadOpenAi(ia.url(), ia.modelo(), ia.clave(), ia.timeoutMs());
            case "simulado" -> new SugeridorGravedadSimulado();
            default -> throw new IllegalStateException("Proveedor de IA desconocido: " + ia.proveedor()
                    + " (use 'simulado' u 'openai')");
        };
        LOG.info("IA habilitada con el proveedor '{}' y espera máxima de {} ms", ia.proveedor(), ia.timeoutMs());
        return new SugeridorConTiempoLimite(modelo, ia.timeoutMs());
    }
}
