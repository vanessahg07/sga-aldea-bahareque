package co.edu.uniquindio.sga.infrastructure.seguridad;

import co.edu.uniquindio.sga.domain.usuario.Usuario;
import co.edu.uniquindio.sga.infrastructure.configuracion.PropiedadesSga;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Emite los JWT de sesión. El token lleva el id del usuario (sub), su nombre y su rol. */
@Component
public class EmisorTokens {

    public static final String CLAIM_ROL = "rol";
    public static final String CLAIM_NOMBRE = "nombre";

    public record Token(String valor, Instant expiraEn) {
    }

    private final JwtEncoder encoder;
    private final PropiedadesSga propiedades;

    public EmisorTokens(JwtEncoder encoder, PropiedadesSga propiedades) {
        this.encoder = encoder;
        this.propiedades = propiedades;
    }

    public Token emitir(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(propiedades.jwt().minutosExpiracion(), ChronoUnit.MINUTES);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(propiedades.jwt().emisor())
                .subject(usuario.getId())
                .issuedAt(ahora)
                .expiresAt(expira)
                .claim(CLAIM_NOMBRE, usuario.getNombre())
                .claim(CLAIM_ROL, usuario.getRol().name())
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        return new Token(encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue(), expira);
    }
}
