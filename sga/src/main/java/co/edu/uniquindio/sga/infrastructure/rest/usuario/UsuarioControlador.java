package co.edu.uniquindio.sga.infrastructure.rest.usuario;

import co.edu.uniquindio.sga.application.usuario.UsuarioServicio;
import co.edu.uniquindio.sga.domain.usuario.Rol;
import co.edu.uniquindio.sga.infrastructure.rest.comun.PaginaRespuesta;
import co.edu.uniquindio.sga.infrastructure.rest.comun.Respuestas.UsuarioR;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** Gestión de usuarios y roles. Solo el administrador asigna roles internos. */
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuarioControlador {

    public record UsuarioSolicitud(@NotBlank String nombre, @NotBlank @Email String correo,
                                   @NotBlank String contrasena, @NotBlank String telefono,
                                   @NotBlank String documento, @NotNull @Past LocalDate fechaNacimiento,
                                   @NotNull Rol rol) {
    }

    public record RolSolicitud(@NotNull Rol rol) {
    }

    private final UsuarioServicio usuarios;

    public UsuarioControlador(UsuarioServicio usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping
    public PaginaRespuesta<UsuarioR> listar(@RequestParam(defaultValue = "0") int pagina) {
        return PaginaRespuesta.de(usuarios.listar(pagina), UsuarioR::de);
    }

    @GetMapping("/{id}")
    public UsuarioR consultar(@PathVariable String id) {
        return UsuarioR.de(usuarios.consultar(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioR crear(@Valid @RequestBody UsuarioSolicitud s) {
        return UsuarioR.de(usuarios.crear(new UsuarioServicio.DatosRegistro(s.nombre(), s.correo(), s.contrasena(),
                s.telefono(), s.documento(), s.fechaNacimiento()), s.rol()));
    }

    @PutMapping("/{id}/rol")
    public UsuarioR asignarRol(@PathVariable String id, @Valid @RequestBody RolSolicitud s) {
        return UsuarioR.de(usuarios.asignarRol(id, s.rol()));
    }

    /** Eliminación lógica. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String id) {
        usuarios.eliminar(id);
    }
}
