package co.edu.uniquindio.sga.infrastructure.imagenes;

import co.edu.uniquindio.sga.domain.apartamento.AlmacenImagenes;
import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import co.edu.uniquindio.sga.infrastructure.configuracion.PropiedadesSga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Almacén de imágenes en Cloudinary (plan gratuito, sin tarjeta). Sube con firma usando la
 * clave y el secreto de la API, que llegan por variables de entorno (I-06). Si Cloudinary no
 * responde a tiempo, el administrador recibe un aviso y puede registrar una URL existente:
 * nada del negocio depende de este servicio (I-05).
 */
public class AlmacenImagenesCloudinary implements AlmacenImagenes {

    private static final Logger LOG = LoggerFactory.getLogger(AlmacenImagenesCloudinary.class);

    private final RestClient cliente;
    private final PropiedadesSga.Cloudinary credenciales;
    private final Clock reloj;

    public AlmacenImagenesCloudinary(PropiedadesSga.Cloudinary credenciales, int timeoutMs, Clock reloj) {
        if (vacio(credenciales.nube()) || vacio(credenciales.claveApi()) || vacio(credenciales.secretoApi())) {
            throw new IllegalStateException("sga.imagenes.proveedor=cloudinary requiere SGA_CLOUDINARY_NUBE, "
                    + "SGA_CLOUDINARY_CLAVE y SGA_CLOUDINARY_SECRETO");
        }
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs)).build());
        fabrica.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.cliente = RestClient.builder()
                .baseUrl("https://api.cloudinary.com/v1_1/" + credenciales.nube())
                .requestFactory(fabrica)
                .build();
        this.credenciales = credenciales;
        this.reloj = reloj;
    }

    @Override
    public String guardar(Archivo archivo) {
        long instante = reloj.instant().getEpochSecond();
        String carpeta = vacio(credenciales.carpeta()) ? "sga" : credenciales.carpeta();
        MultiValueMap<String, Object> formulario = new LinkedMultiValueMap<>();
        formulario.add("file", new ByteArrayResource(archivo.contenido()) {
            @Override
            public String getFilename() {
                return archivo.nombre();
            }
        });
        formulario.add("api_key", credenciales.claveApi());
        formulario.add("timestamp", String.valueOf(instante));
        formulario.add("folder", carpeta);
        formulario.add("signature", firmar("folder=" + carpeta + "&timestamp=" + instante));
        try {
            JsonNode respuesta = cliente.post().uri("/image/upload").contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(formulario).retrieve().body(JsonNode.class);
            String url = respuesta == null ? "" : respuesta.path("secure_url").asString("");
            if (url.isBlank()) {
                throw new ExcepcionNegocio(CodigoError.SERVICIO_EXTERNO_NO_DISPONIBLE, "Cloudinary no devolvió la URL");
            }
            return url;
        } catch (RestClientException e) {
            LOG.warn("No se pudo subir la imagen a Cloudinary: {}", e.getMessage());
            throw new ExcepcionNegocio(CodigoError.SERVICIO_EXTERNO_NO_DISPONIBLE,
                    "no se pudo subir la imagen; puede registrar la URL de una imagen ya publicada");
        }
    }

    /** Firma de Cloudinary: SHA-1 de los parámetros ordenados seguidos del secreto de la API. */
    private String firmar(String parametros) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-1")
                    .digest((parametros + credenciales.secretoApi()).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
