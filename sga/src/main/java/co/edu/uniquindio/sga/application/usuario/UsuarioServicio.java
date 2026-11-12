package co.edu.uniquindio.sga.application.usuario;

import co.edu.uniquindio.sga.domain.comun.*;
import co.edu.uniquindio.sga.domain.reserva.ReservaRepositorio;
import co.edu.uniquindio.sga.domain.usuario.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** Casos de uso de cuentas de usuario: registro, acceso, perfil, recuperación y administración. */
@Service
@Transactional
public class UsuarioServicio {

    private static final Logger LOG = LoggerFactory.getLogger(UsuarioServicio.class);

    public record DatosRegistro(String nombre, String correo, String contrasena, String telefono, String documento,
                                LocalDate fechaNacimiento) {
    }

    private final UsuarioRepositorio usuarios;
    private final CodigoRecuperacionRepositorio codigos;
    private final ReservaRepositorio reservas;
    private final CifradorContrasenas cifrador;
    private final Notificador notificador;
    private final Reloj reloj;

    public UsuarioServicio(UsuarioRepositorio usuarios, CodigoRecuperacionRepositorio codigos,
                           ReservaRepositorio reservas, CifradorContrasenas cifrador, Notificador notificador,
                           Reloj reloj) {
        this.usuarios = usuarios;
        this.codigos = codigos;
        this.reservas = reservas;
        this.cifrador = cifrador;
        this.notificador = notificador;
        this.reloj = reloj;
    }

    /** Registro público: siempre crea un huésped. */
    public Usuario registrarHuesped(DatosRegistro datos) {
        return crear(datos, Rol.HUESPED);
    }

    /** Solo el administrador crea cuentas con roles internos. */
    public Usuario crear(DatosRegistro datos, Rol rol) {
        ExcepcionNegocio.exigir(!usuarios.existeCorreo(Usuario.normalizarCorreo(datos.correo())),
                CodigoError.CORREO_YA_REGISTRADO);
        Usuario usuario = Usuario.crear(datos.nombre(), datos.correo(), datos.contrasena(), datos.telefono(),
                datos.documento(), datos.fechaNacimiento(), rol, cifrador);
        usuarios.guardar(usuario);
        return usuario;
    }

    @Transactional(readOnly = true)
    public Usuario autenticar(String correo, String contrasena) {
        return usuarios.buscarPorCorreo(Usuario.normalizarCorreo(correo))
                .filter(u -> u.autenticar(contrasena, cifrador))
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.CREDENCIALES_INVALIDAS));
    }

    @Transactional(readOnly = true)
    public Usuario consultar(String id) {
        return usuarios.buscar(id).filter(u -> !u.isEliminado())
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.USUARIO_NO_ENCONTRADO));
    }

    public Usuario actualizarPerfil(String id, String nombre, String telefono, String fotoUrl) {
        Usuario usuario = consultar(id);
        usuario.actualizarPerfil(nombre, telefono, fotoUrl);
        usuarios.guardar(usuario);
        return usuario;
    }

    public void cambiarContrasena(String id, String actual, String nueva) {
        Usuario usuario = consultar(id);
        usuario.cambiarContrasena(actual, nueva, cifrador);
        usuarios.guardar(usuario);
    }

    /**
     * Emite un código de un solo uso con vigencia de 15 minutos y lo envía al correo.
     * Si el correo no está registrado no se informa, para no revelar qué cuentas existen.
     */
    public void solicitarRecuperacion(String correo) {
        String normalizado = Usuario.normalizarCorreo(correo);
        if (usuarios.buscarPorCorreo(normalizado).isEmpty()) {
            return;
        }
        CodigoRecuperacion.Emision emision = CodigoRecuperacion.emitir(normalizado, reloj.ahora(), cifrador);
        codigos.guardar(emision.codigo());
        try {
            notificador.notificar(new Notificador.Notificacion(normalizado, "Código de recuperación",
                    "Su código para restablecer la contraseña es " + emision.codigoEnClaro()
                            + ". Vence en 15 minutos y solo puede usarse una vez."));
        } catch (RuntimeException e) {
            LOG.warn("No se pudo enviar el código de recuperación a {}: {}", normalizado, e.getMessage());
        }
    }

    public void restablecerContrasena(String correo, String codigo, String nueva) {
        String normalizado = Usuario.normalizarCorreo(correo);
        CodigoRecuperacion recuperacion = codigos.ultimoDe(normalizado)
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.CODIGO_RECUPERACION_INVALIDO));
        Usuario usuario = usuarios.buscarPorCorreo(normalizado)
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.CODIGO_RECUPERACION_INVALIDO));
        Usuario.validarContrasena(nueva);
        recuperacion.usar(codigo, reloj.ahora(), cifrador);
        usuario.restablecerContrasena(nueva, cifrador);
        codigos.guardar(recuperacion);
        usuarios.guardar(usuario);
    }

    // --- Administración ---

    @Transactional(readOnly = true)
    public Pagina<Usuario> listar(int pagina) {
        return usuarios.listar(pagina);
    }

    public Usuario asignarRol(String id, Rol rol) {
        Usuario usuario = consultar(id);
        usuario.asignarRol(rol);
        usuarios.guardar(usuario);
        return usuario;
    }

    /** Eliminación lógica; no procede si es titular de reservas activas. Sus reservas se conservan. */
    public void eliminar(String id) {
        Usuario usuario = consultar(id);
        usuario.eliminar(reservas.existenActivasDeCuenta(usuario.getId()));
        usuarios.guardar(usuario);
    }
}
