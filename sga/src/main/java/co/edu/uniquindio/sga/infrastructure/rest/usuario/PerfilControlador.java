package co.edu.uniquindio.sga.infrastructure.rest.usuario;

import co.edu.uniquindio.sga.application.usuario.UsuarioServicio;
import co.edu.uniquindio.sga.infrastructure.rest.comun.Respuestas.MensajeR;
import co.edu.uniquindio.sga.infrastructure.rest.comun.Respuestas.UsuarioR;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** Perfil del usuario autenticado. El correo no es editable. */
@RestController
@RequestMapping("/api/perfil")
public class PerfilControlador {

    public record PerfilSolicitud(@NotBlank String nombre, @NotBlank String telefono, String fotoUrl) {
    }

    public record CambioContrasenaSolicitud(@NotBlank String contrasenaActual, @NotBlank String nuevaContrasena) {
    }

    private final UsuarioServicio usuarios;

    public PerfilControlador(UsuarioServicio usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping
    public UsuarioR consultar(@AuthenticationPrincipal Jwt jwt) {
        return UsuarioR.de(usuarios.consultar(jwt.getSubject()));
    }

    @PutMapping
    public UsuarioR actualizar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PerfilSolicitud s) {
        return UsuarioR.de(usuarios.actualizarPerfil(jwt.getSubject(), s.nombre(), s.telefono(), s.fotoUrl()));
    }

    @PutMapping("/contrasena")
    public MensajeR cambiarContrasena(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CambioContrasenaSolicitud s) {
        usuarios.cambiarContrasena(jwt.getSubject(), s.contrasenaActual(), s.nuevaContrasena());
        return new MensajeR("La contraseña se actualizó.");
    }
}
