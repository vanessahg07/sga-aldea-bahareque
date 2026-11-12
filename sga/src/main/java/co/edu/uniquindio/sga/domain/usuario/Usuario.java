package co.edu.uniquindio.sga.domain.usuario;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;

import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Cuenta de acceso al sistema: credenciales y rol. Es un concepto de acceso, distinto
 * del titular de una reserva; el resto del dominio no depende de él.
 */
public class Usuario {

    private static final Pattern CORREO = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)*\\.[A-Za-z]{2,}$");

    private final String id;
    private String nombre;
    private final String correo;
    private String contrasenaCifrada;
    private String telefono;
    private final String documento;
    private final LocalDate fechaNacimiento;
    private String fotoUrl;
    private Rol rol;
    private boolean eliminado;

    private Usuario(String id, String nombre, String correo, String contrasenaCifrada, String telefono,
                    String documento, LocalDate fechaNacimiento, String fotoUrl, Rol rol, boolean eliminado) {
        ExcepcionNegocio.exigir(correo != null && CORREO.matcher(correo).matches(), CodigoError.CORREO_INVALIDO);
        ExcepcionNegocio.exigir(documento != null && !documento.isBlank(), CodigoError.USUARIO_INVALIDO, "documento");
        ExcepcionNegocio.exigir(fechaNacimiento != null, CodigoError.USUARIO_INVALIDO, "fecha de nacimiento");
        ExcepcionNegocio.exigir(contrasenaCifrada != null && !contrasenaCifrada.isBlank(),
                CodigoError.USUARIO_INVALIDO, "contraseña");
        ExcepcionNegocio.exigir(rol != null, CodigoError.USUARIO_INVALIDO, "rol");
        this.id = id;
        this.correo = correo.trim().toLowerCase();
        this.contrasenaCifrada = contrasenaCifrada;
        this.documento = documento;
        this.fechaNacimiento = fechaNacimiento;
        this.rol = rol;
        this.eliminado = eliminado;
        actualizarPerfil(nombre, telefono, fotoUrl);
    }

    /**
     * Crea una cuenta nueva. La contraseña llega en claro, se valida y se guarda cifrada.
     * El registro público solo crea huéspedes; los roles internos los asigna un administrador.
     */
    public static Usuario crear(String nombre, String correo, String contrasena, String telefono, String documento,
                                LocalDate fechaNacimiento, Rol rol, CifradorContrasenas cifrador) {
        validarContrasena(contrasena);
        return new Usuario(UUID.randomUUID().toString(), nombre, correo, cifrador.cifrar(contrasena), telefono,
                documento, fechaNacimiento, null, rol, false);
    }

    public static Usuario reconstituir(String id, String nombre, String correo, String contrasenaCifrada,
                                       String telefono, String documento, LocalDate fechaNacimiento, String fotoUrl,
                                       Rol rol, boolean eliminado) {
        return new Usuario(id, nombre, correo, contrasenaCifrada, telefono, documento, fechaNacimiento, fotoUrl, rol,
                eliminado);
    }

    /** Mínimo 8 caracteres, con al menos una mayúscula y un número. */
    public static void validarContrasena(String contrasena) {
        ExcepcionNegocio.exigir(contrasena != null && contrasena.length() >= 8
                        && contrasena.chars().anyMatch(Character::isUpperCase)
                        && contrasena.chars().anyMatch(Character::isDigit),
                CodigoError.CONTRASENA_INSEGURA);
    }

    public static String normalizarCorreo(String correo) {
        return correo == null ? null : correo.trim().toLowerCase();
    }

    /** El correo no es editable. */
    public void actualizarPerfil(String nombre, String telefono, String fotoUrl) {
        ExcepcionNegocio.exigir(nombre != null && !nombre.isBlank(), CodigoError.USUARIO_INVALIDO, "nombre");
        ExcepcionNegocio.exigir(telefono != null && !telefono.isBlank(), CodigoError.USUARIO_INVALIDO, "teléfono");
        this.nombre = nombre;
        this.telefono = telefono;
        this.fotoUrl = fotoUrl;
    }

    public boolean autenticar(String contrasena, CifradorContrasenas cifrador) {
        return !eliminado && contrasena != null && cifrador.coincide(contrasena, contrasenaCifrada);
    }

    /** Cambio voluntario: exige la contraseña actual. */
    public void cambiarContrasena(String actual, String nueva, CifradorContrasenas cifrador) {
        ExcepcionNegocio.exigir(cifrador.coincide(actual, contrasenaCifrada), CodigoError.CONTRASENA_ACTUAL_INCORRECTA);
        restablecerContrasena(nueva, cifrador);
    }

    /** Restablecimiento tras validar un código de recuperación. */
    public void restablecerContrasena(String nueva, CifradorContrasenas cifrador) {
        validarContrasena(nueva);
        this.contrasenaCifrada = cifrador.cifrar(nueva);
    }

    public void asignarRol(Rol nuevoRol) {
        ExcepcionNegocio.exigir(nuevoRol != null, CodigoError.USUARIO_INVALIDO, "rol");
        this.rol = nuevoRol;
    }

    /** Eliminación lógica. No procede si es titular de reservas activas; sus reservas históricas se conservan. */
    public void eliminar(boolean esTitularDeReservasActivas) {
        ExcepcionNegocio.exigir(!esTitularDeReservasActivas, CodigoError.USUARIO_CON_RESERVAS_ACTIVAS);
        this.eliminado = true;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getContrasenaCifrada() { return contrasenaCifrada; }
    public String getTelefono() { return telefono; }
    public String getDocumento() { return documento; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public String getFotoUrl() { return fotoUrl; }
    public Rol getRol() { return rol; }
    public boolean isEliminado() { return eliminado; }
}
