package co.edu.uniquindio.sga.infrastructure.rest.usuario;

import co.edu.uniquindio.sga.application.usuario.UsuarioServicio;
import co.edu.uniquindio.sga.domain.usuario.Usuario;
import co.edu.uniquindio.sga.infrastructure.rest.comun.Respuestas.MensajeR;
import co.edu.uniquindio.sga.infrastructure.rest.comun.Respuestas.SesionR;
import co.edu.uniquindio.sga.infrastructure.rest.comun.Respuestas.UsuarioR;
import co.edu.uniquindio.sga.infrastructure.seguridad.EmisorTokens;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** Registro público, inicio de sesión y recuperación de contraseña. */
@RestController
@RequestMapping("/api/auth")
public class AutenticacionControlador {

    public record RegistroSolicitud(@NotBlank String nombre, @NotBlank @Email String correo,
                                    @NotBlank String contrasena, @NotBlank String telefono,
                                    @NotBlank String documento, @NotNull @Past LocalDate fechaNacimiento) {
    }

    public record LoginSolicitud(@NotBlank String correo, @NotBlank String contrasena) {
    }

    public record RecuperacionSolicitud(@NotBlank @Email String correo) {
    }

    public record RestablecimientoSolicitud(@NotBlank @Email String correo, @NotBlank String codigo,
                                            @NotBlank String nuevaContrasena) {
    }

    private final UsuarioServicio usuarios;
    private final EmisorTokens tokens;

    public AutenticacionControlador(UsuarioServicio usuarios, EmisorTokens tokens) {
        this.usuarios = usuarios;
        this.tokens = tokens;
    }

    /** El registro público crea siempre un huésped y devuelve la sesión iniciada. */
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public SesionR registrar(@Valid @RequestBody RegistroSolicitud s) {
        Usuario usuario = usuarios.registrarHuesped(new UsuarioServicio.DatosRegistro(s.nombre(), s.correo(),
                s.contrasena(), s.telefono(), s.documento(), s.fechaNacimiento()));
        return sesion(usuario);
    }

    @PostMapping("/login")
    public SesionR iniciarSesion(@Valid @RequestBody LoginSolicitud s) {
        return sesion(usuarios.autenticar(s.correo(), s.contrasena()));
    }

    /** Siempre responde igual, exista o no la cuenta, para no revelar qué correos están registrados. */
    @PostMapping("/recuperacion")
    public MensajeR solicitarRecuperacion(@Valid @RequestBody RecuperacionSolicitud s) {
        usuarios.solicitarRecuperacion(s.correo());
        return new MensajeR("Si el correo está registrado, recibirá un código válido por 15 minutos.");
    }

    @PostMapping("/recuperacion/restablecimiento")
    public MensajeR restablecer(@Valid @RequestBody RestablecimientoSolicitud s) {
        usuarios.restablecerContrasena(s.correo(), s.codigo(), s.nuevaContrasena());
        return new MensajeR("La contraseña se actualizó. Ya puede iniciar sesión.");
    }

    private SesionR sesion(Usuario usuario) {
        EmisorTokens.Token token = tokens.emitir(usuario);
        return new SesionR(token.valor(), "Bearer", token.expiraEn(), UsuarioR.de(usuario));
    }
}
