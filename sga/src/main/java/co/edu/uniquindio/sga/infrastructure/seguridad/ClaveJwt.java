package co.edu.uniquindio.sga.infrastructure.seguridad;

import co.edu.uniquindio.sga.infrastructure.configuracion.PropiedadesSga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Clave HMAC-SHA256 con la que se firman y validan los JWT.
 *
 * <p>Se toma de la variable de entorno {@code SGA_JWT_SECRETO} y nunca se versiona. En los
 * perfiles de despliegue el secreto es obligatorio y la aplicación no arranca sin él. En
 * desarrollo, si falta, se genera una clave aleatoria: los tokens dejan de valer al reiniciar.</p>
 */
@Component
public class ClaveJwt {

    private static final Logger LOG = LoggerFactory.getLogger(ClaveJwt.class);
    private static final int LONGITUD_MINIMA = 32;

    private final SecretKey clave;

    public ClaveJwt(PropiedadesSga propiedades) {
        String secreto = propiedades.jwt().secreto();
        byte[] bytes;
        if (secreto == null || secreto.isBlank()) {
            if (propiedades.jwt().secretoObligatorio()) {
                throw new IllegalStateException("Falta SGA_JWT_SECRETO: es obligatorio en el despliegue");
            }
            bytes = new byte[LONGITUD_MINIMA];
            new SecureRandom().nextBytes(bytes);
            LOG.warn("SGA_JWT_SECRETO no está configurado: se generó una clave temporal para esta ejecución.");
        } else {
            bytes = secreto.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < LONGITUD_MINIMA) {
                throw new IllegalStateException("SGA_JWT_SECRETO debe tener al menos " + LONGITUD_MINIMA + " caracteres");
            }
        }
        this.clave = new SecretKeySpec(bytes, "HmacSHA256");
    }

    public SecretKey clave() {
        return clave;
    }
}
