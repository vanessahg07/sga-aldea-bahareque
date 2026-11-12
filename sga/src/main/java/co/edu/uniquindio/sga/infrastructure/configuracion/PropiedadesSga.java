package co.edu.uniquindio.sga.infrastructure.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración técnica del SGA (prefijo {@code sga}). Los valores del negocio no viven
 * aquí: son datos del alojamiento que el administrador edita. Las credenciales de las
 * integraciones se toman de variables de entorno y nunca se versionan (I-06).
 */
@ConfigurationProperties(prefix = "sga")
public record PropiedadesSga(String zonaHoraria, Jwt jwt, DatosPrueba datosPrueba, Reservas reservas,
                             Notificaciones notificaciones, Ia ia, Imagenes imagenes, Frontend frontend) {

    public PropiedadesSga {
        zonaHoraria = zonaHoraria == null ? "America/Bogota" : zonaHoraria;
        jwt = jwt == null ? new Jwt(null, 480, "sga-aldea-bahareque") : jwt;
        datosPrueba = datosPrueba == null ? new DatosPrueba(true, null, null) : datosPrueba;
        reservas = reservas == null ? new Reservas(60_000) : reservas;
        notificaciones = notificaciones == null ? new Notificaciones("memoria", null, 10_000, null) : notificaciones;
        ia = ia == null ? new Ia(false, "simulado", null, null, null, 5_000) : ia;
        imagenes = imagenes == null ? new Imagenes("local", null, 15_000, null) : imagenes;
        frontend = frontend == null ? new Frontend(null) : frontend;
    }

    /**
     * @param secreto clave de firma HMAC (mínimo 32 caracteres). Se toma de la variable de
     *                entorno SGA_JWT_SECRETO; si falta, se genera una al arrancar.
     */
    public record Jwt(String secreto, long minutosExpiracion, String emisor) {
    }

    /**
     * @param contrasena      contraseña de los usuarios de demostración
     * @param credencialCanal credencial del canal de demostración; si falta, se genera y se muestra en el log
     */
    public record DatosPrueba(boolean habilitados, String contrasena, String credencialCanal) {
    }

    /** @param intervaloVencimientoMs cada cuánto se revisan las reservas pendientes vencidas */
    public record Reservas(long intervaloVencimientoMs) {
    }

    /**
     * @param proveedor {@code memoria} (local, sin conexión) o {@code smtp}
     * @param remitente dirección que aparece como remitente
     * @param timeoutMs tiempo máximo de conexión, lectura y escritura con el servidor de correo
     */
    public record Notificaciones(String proveedor, String remitente, int timeoutMs, Smtp smtp) {

        public Notificaciones {
            proveedor = proveedor == null || proveedor.isBlank() ? "memoria" : proveedor;
            timeoutMs = timeoutMs <= 0 ? 10_000 : timeoutMs;
            smtp = smtp == null ? new Smtp(null, 587, null, null, true) : smtp;
        }
    }

    public record Smtp(String host, int puerto, String usuario, String clave, boolean starttls) {
    }

    /**
     * Modelo de IA no bloqueante (10.2).
     *
     * @param habilitada {@code false} desactiva la IA sin tocar código
     * @param proveedor  {@code simulado} (local, sin conexión) u {@code openai} (cualquier API compatible
     *                   con /chat/completions: un modelo local con Ollama o un proveedor con nivel gratuito)
     * @param url        URL base de la API, por ejemplo {@code http://localhost:11434/v1}
     * @param clave      clave de la API, desde la variable de entorno SGA_IA_CLAVE (vacía para modelos locales)
     * @param timeoutMs  tiempo máximo de espera de la respuesta del modelo
     */
    public record Ia(boolean habilitada, String proveedor, String url, String modelo, String clave, int timeoutMs) {

        public Ia {
            proveedor = proveedor == null || proveedor.isBlank() ? "simulado" : proveedor;
            timeoutMs = timeoutMs <= 0 ? 5_000 : timeoutMs;
        }
    }

    /**
     * @param proveedor  {@code local} (disco del servidor) o {@code cloudinary}
     * @param directorio carpeta donde el almacén local guarda los archivos
     * @param timeoutMs  tiempo máximo de la subida al servicio externo
     */
    public record Imagenes(String proveedor, String directorio, int timeoutMs, Cloudinary cloudinary) {

        public Imagenes {
            proveedor = proveedor == null || proveedor.isBlank() ? "local" : proveedor;
            directorio = directorio == null || directorio.isBlank() ? "./datos/imagenes" : directorio;
            timeoutMs = timeoutMs <= 0 ? 15_000 : timeoutMs;
            cloudinary = cloudinary == null ? new Cloudinary(null, null, null, "sga") : cloudinary;
        }
    }

    /** Credenciales de Cloudinary, desde variables de entorno. */
    public record Cloudinary(String nube, String claveApi, String secretoApi, String carpeta) {
    }

    /** @param directorio carpeta con el frontend compilado para servirlo desde el mismo servidor (despliegue) */
    public record Frontend(String directorio) {
    }
}
