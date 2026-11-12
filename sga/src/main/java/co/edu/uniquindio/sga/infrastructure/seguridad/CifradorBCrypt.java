package co.edu.uniquindio.sga.infrastructure.seguridad;

import co.edu.uniquindio.sga.domain.usuario.CifradorContrasenas;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Adaptador del puerto {@link CifradorContrasenas} con BCrypt. */
@Component
public class CifradorBCrypt implements CifradorContrasenas {

    private final PasswordEncoder encoder;

    public CifradorBCrypt(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public String cifrar(String textoPlano) {
        return encoder.encode(textoPlano);
    }

    @Override
    public boolean coincide(String textoPlano, String cifrado) {
        return textoPlano != null && cifrado != null && encoder.matches(textoPlano, cifrado);
    }
}
