package co.edu.uniquindio.sga.infrastructure.rest.comun;

import java.time.Instant;
import java.util.List;

/** Error con significado de negocio: un código estable, un mensaje legible y, si aplica, el detalle por campo. */
public record ErrorRespuesta(String codigo, String mensaje, List<String> detalles, Instant fecha) {

    public static ErrorRespuesta de(String codigo, String mensaje, List<String> detalles) {
        return new ErrorRespuesta(codigo, mensaje, detalles, Instant.now());
    }
}
