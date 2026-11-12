package co.edu.uniquindio.sga.domain.canal;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import co.edu.uniquindio.sga.domain.usuario.CifradorContrasenas;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * Canal de venta externo registrado por el administrador. Se autentica ante el SGA con
 * una credencial propia que se guarda cifrada y solo se muestra al generarla.
 */
public class CanalVenta {

    private static final Pattern CODIGO = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{1,29}");
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final int BYTES_CREDENCIAL = 24;

    private final String codigo;
    private String nombre;
    private TipoCanal tipo;
    private String credencialCifrada;
    private boolean activo;

    private CanalVenta(String codigo, String nombre, TipoCanal tipo, String credencialCifrada, boolean activo) {
        ExcepcionNegocio.exigir(codigo != null && CODIGO.matcher(codigo).matches(), CodigoError.CANAL_INVALIDO,
                "el código admite letras, números, guion y guion bajo (2 a 30 caracteres)");
        this.codigo = codigo;
        this.credencialCifrada = credencialCifrada;
        this.activo = activo;
        actualizarDatos(nombre, tipo);
    }

    /** Resultado de registrar un canal o de renovar su credencial: la credencial en claro se entrega una sola vez. */
    public record Credencial(CanalVenta canal, String credencialEnClaro) {
    }

    public static Credencial registrar(String codigo, String nombre, TipoCanal tipo, CifradorContrasenas cifrador) {
        String enClaro = nuevaCredencial();
        return new Credencial(new CanalVenta(codigo, nombre, tipo, cifrador.cifrar(enClaro), true), enClaro);
    }

    /** Registra un canal con una credencial conocida (datos de demostración configurados externamente). */
    public static CanalVenta registrarConCredencial(String codigo, String nombre, TipoCanal tipo, String credencial,
                                                    CifradorContrasenas cifrador) {
        ExcepcionNegocio.exigir(credencial != null && credencial.length() >= 16, CodigoError.CANAL_INVALIDO,
                "la credencial debe tener al menos 16 caracteres");
        return new CanalVenta(codigo, nombre, tipo, cifrador.cifrar(credencial), true);
    }

    public static CanalVenta reconstituir(String codigo, String nombre, TipoCanal tipo, String credencialCifrada,
                                          boolean activo) {
        return new CanalVenta(codigo, nombre, tipo, credencialCifrada, activo);
    }

    public void actualizarDatos(String nombre, TipoCanal tipo) {
        ExcepcionNegocio.exigir(nombre != null && !nombre.isBlank() && tipo != null, CodigoError.CANAL_INVALIDO,
                "nombre y tipo son obligatorios");
        this.nombre = nombre.trim();
        this.tipo = tipo;
    }

    /** Verifica la credencial presentada. Un canal inactivo no puede operar aunque la credencial sea correcta. */
    public void autenticar(String credencial, CifradorContrasenas cifrador) {
        ExcepcionNegocio.exigir(credencial != null && cifrador.coincide(credencial, credencialCifrada),
                CodigoError.CREDENCIAL_CANAL_INVALIDA);
        ExcepcionNegocio.exigir(activo, CodigoError.CANAL_INACTIVO);
    }

    /** Invalida la credencial anterior y devuelve la nueva en claro. */
    public String renovarCredencial(CifradorContrasenas cifrador) {
        String enClaro = nuevaCredencial();
        credencialCifrada = cifrador.cifrar(enClaro);
        return enClaro;
    }

    public void desactivar() {
        activo = false;
    }

    public void activar() {
        activo = true;
    }

    private static String nuevaCredencial() {
        byte[] bytes = new byte[BYTES_CREDENCIAL];
        ALEATORIO.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public TipoCanal getTipo() { return tipo; }
    public String getCredencialCifrada() { return credencialCifrada; }
    public boolean isActivo() { return activo; }
}
