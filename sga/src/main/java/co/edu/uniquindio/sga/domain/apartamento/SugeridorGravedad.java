package co.edu.uniquindio.sga.domain.apartamento;

import java.util.Optional;

/**
 * Puerto del modelo de IA que sugiere la gravedad de una novedad a partir de su
 * descripción (L-21). Es una ayuda <b>no bloqueante</b>: si el modelo está deshabilitado,
 * falla o tarda, devuelve vacío y el personal elige la gravedad por su cuenta.
 */
public interface SugeridorGravedad {

    Optional<SugerenciaGravedad> sugerir(String descripcion);

    /**
     * @param justificacion explicación breve del modelo, para que la persona decida si la acepta
     * @param proveedor     modelo o implementación que produjo la sugerencia
     */
    record SugerenciaGravedad(Novedad.Gravedad gravedad, String justificacion, String proveedor) {
    }
}
