package co.edu.uniquindio.sga.domain.folio;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;

import java.time.LocalDateTime;

/** Autorización explícita del administrador para cerrar un folio con saldo distinto de cero. */
public record AutorizacionCierre(String autor, String motivo, LocalDateTime fecha) {

    public AutorizacionCierre {
        ExcepcionNegocio.exigir(autor != null && !autor.isBlank() && motivo != null && !motivo.isBlank()
                && fecha != null, CodigoError.AUTORIZACION_INVALIDA);
    }
}
