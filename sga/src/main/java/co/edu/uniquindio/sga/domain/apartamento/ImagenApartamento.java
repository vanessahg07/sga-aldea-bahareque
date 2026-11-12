package co.edu.uniquindio.sga.domain.apartamento;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;

/** Imagen de un apartamento, almacenada en un servicio externo y referenciada por su URL. */
public record ImagenApartamento(String url, boolean principal) {

    public ImagenApartamento {
        ExcepcionNegocio.exigir(url != null && !url.isBlank(), CodigoError.IMAGENES_INVALIDAS, "URL vacía");
    }
}
