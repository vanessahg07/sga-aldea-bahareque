package co.edu.uniquindio.sga.infrastructure.seguridad;

import co.edu.uniquindio.sga.application.comun.Solicitante;
import co.edu.uniquindio.sga.domain.usuario.Rol;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Traduce el JWT autenticado en el {@link Solicitante} que reciben los casos de uso. */
@Component
public class SolicitanteActual {

    public Solicitante de(Jwt jwt) {
        return new Solicitante(jwt.getSubject(), jwt.getClaimAsString(EmisorTokens.CLAIM_NOMBRE),
                Rol.valueOf(jwt.getClaimAsString(EmisorTokens.CLAIM_ROL)));
    }
}
