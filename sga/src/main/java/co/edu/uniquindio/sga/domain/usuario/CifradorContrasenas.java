package co.edu.uniquindio.sga.domain.usuario;

/** Puerto para cifrar contraseñas y códigos de un solo uso. El dominio nunca guarda secretos en claro. */
public interface CifradorContrasenas {

    String cifrar(String textoPlano);

    boolean coincide(String textoPlano, String cifrado);
}
