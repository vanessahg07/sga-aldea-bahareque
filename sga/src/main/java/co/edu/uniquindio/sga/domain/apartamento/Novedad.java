package co.edu.uniquindio.sga.domain.apartamento;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;

import java.time.LocalDateTime;
import java.util.UUID;

/** Reporte de un daño, faltante o situación en un apartamento. */
public record Novedad(String id, String codigoApartamento, LocalDateTime fecha, String autor, String descripcion,
                      Gravedad gravedad) {

    public enum Gravedad { BAJA, MEDIA, ALTA, CRITICA }

    public Novedad {
        ExcepcionNegocio.exigir(id != null && codigoApartamento != null && fecha != null, CodigoError.NOVEDAD_INVALIDA);
        ExcepcionNegocio.exigir(autor != null && !autor.isBlank(), CodigoError.NOVEDAD_INVALIDA, "autor");
        ExcepcionNegocio.exigir(descripcion != null && !descripcion.isBlank(), CodigoError.NOVEDAD_INVALIDA, "descripción");
        ExcepcionNegocio.exigir(gravedad != null, CodigoError.NOVEDAD_INVALIDA, "gravedad");
    }

    public static Novedad registrar(String codigoApartamento, LocalDateTime fecha, String autor, String descripcion,
                                    Gravedad gravedad) {
        return new Novedad(UUID.randomUUID().toString(), codigoApartamento, fecha, autor, descripcion, gravedad);
    }
}
