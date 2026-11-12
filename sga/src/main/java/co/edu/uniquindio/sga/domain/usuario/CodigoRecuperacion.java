package co.edu.uniquindio.sga.domain.usuario;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Código de un solo uso para recuperar la contraseña. Vence a los 15 minutos y se
 * guarda cifrado.
 */
public class CodigoRecuperacion {

    public static final Duration VIGENCIA = Duration.ofMinutes(15);
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final String id;
    private final String correo;
    private final String codigoCifrado;
    private final LocalDateTime expiraEn;
    private boolean usado;

    private CodigoRecuperacion(String id, String correo, String codigoCifrado, LocalDateTime expiraEn, boolean usado) {
        this.id = id;
        this.correo = correo;
        this.codigoCifrado = codigoCifrado;
        this.expiraEn = expiraEn;
        this.usado = usado;
    }

    /** Resultado de emitir un código: el código en claro solo existe para enviarlo al correo. */
    public record Emision(CodigoRecuperacion codigo, String codigoEnClaro) {
    }

    public static Emision emitir(String correo, LocalDateTime ahora, CifradorContrasenas cifrador) {
        String enClaro = String.format("%06d", ALEATORIO.nextInt(1_000_000));
        CodigoRecuperacion codigo = new CodigoRecuperacion(UUID.randomUUID().toString(),
                Usuario.normalizarCorreo(correo), cifrador.cifrar(enClaro), ahora.plus(VIGENCIA), false);
        return new Emision(codigo, enClaro);
    }

    public static CodigoRecuperacion reconstituir(String id, String correo, String codigoCifrado,
                                                  LocalDateTime expiraEn, boolean usado) {
        return new CodigoRecuperacion(id, correo, codigoCifrado, expiraEn, usado);
    }

    /** Valida el código y lo marca como usado. Un código vencido o ya usado se rechaza. */
    public void usar(String codigoEnClaro, LocalDateTime ahora, CifradorContrasenas cifrador) {
        ExcepcionNegocio.exigir(!usado && !ahora.isAfter(expiraEn) && codigoEnClaro != null
                && cifrador.coincide(codigoEnClaro, codigoCifrado), CodigoError.CODIGO_RECUPERACION_INVALIDO);
        usado = true;
    }

    public String getId() { return id; }
    public String getCorreo() { return correo; }
    public String getCodigoCifrado() { return codigoCifrado; }
    public LocalDateTime getExpiraEn() { return expiraEn; }
    public boolean isUsado() { return usado; }
}
