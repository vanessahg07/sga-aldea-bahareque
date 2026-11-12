package co.edu.uniquindio.sga.infrastructure.ia;

import co.edu.uniquindio.sga.domain.apartamento.Novedad;
import co.edu.uniquindio.sga.domain.apartamento.SugeridorGravedad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Modelo de IA real por medio de una API compatible con {@code /chat/completions}. Sirve
 * con un modelo local (Ollama: {@code http://localhost:11434/v1}) o con un proveedor con
 * nivel gratuito (por ejemplo Groq o Gemini en modo compatible). La clave llega por la
 * variable de entorno SGA_IA_CLAVE y nunca se versiona.
 * <p>
 * Si el modelo no responde a tiempo, responde con un error o con algo que no se puede
 * interpretar, devuelve vacío: la sugerencia simplemente no aparece.
 */
public class SugeridorGravedadOpenAi implements SugeridorGravedad {

    private static final Logger LOG = LoggerFactory.getLogger(SugeridorGravedadOpenAi.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String INSTRUCCIONES = """
            Eres el asistente de mantenimiento de un alojamiento turístico en Colombia. Clasifica la novedad \
            reportada por el personal según su gravedad:
            - CRITICA: riesgo para las personas o el apartamento no puede habitarse (gas, incendio, sin agua, inundación).
            - ALTA: falla de un servicio esencial que requiere reparación pronto (nevera, ducha, calentador, fuga).
            - MEDIA: afecta la comodidad pero no impide la estancia (bombillo, wifi, faltante menor).
            - BAJA: detalle estético o menor.
            Responde solo con JSON, sin texto adicional: {"gravedad":"BAJA|MEDIA|ALTA|CRITICA","justificacion":"una frase"}""";

    private final RestClient cliente;
    private final String modelo;

    public SugeridorGravedadOpenAi(String url, String modelo, String clave, int timeoutMs) {
        if (url == null || url.isBlank() || modelo == null || modelo.isBlank()) {
            throw new IllegalStateException("sga.ia.proveedor=openai requiere sga.ia.url y sga.ia.modelo");
        }
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs)).build());
        fabrica.setReadTimeout(Duration.ofMillis(timeoutMs));
        RestClient.Builder constructor = RestClient.builder().baseUrl(url.replaceAll("/+$", "")).requestFactory(fabrica);
        if (clave != null && !clave.isBlank()) {
            constructor.defaultHeader("Authorization", "Bearer " + clave);
        }
        this.cliente = constructor.build();
        this.modelo = modelo;
    }

    @Override
    public Optional<SugerenciaGravedad> sugerir(String descripcion) {
        Map<String, Object> cuerpo = Map.of("model", modelo, "temperature", 0,
                "messages", List.of(Map.of("role", "system", "content", INSTRUCCIONES),
                        Map.of("role", "user", "content", "Novedad: " + descripcion)));
        try {
            String respuesta = cliente.post().uri("/chat/completions").contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo).retrieve().body(String.class);
            return interpretar(respuesta);
        } catch (RestClientException | JacksonException e) {
            LOG.warn("El modelo de IA no respondió correctamente: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** Extrae la clasificación del primer mensaje del modelo, tolerando texto alrededor del JSON. */
    Optional<SugerenciaGravedad> interpretar(String respuesta) {
        if (respuesta == null) {
            return Optional.empty();
        }
        JsonNode raiz = JSON.readTree(respuesta);
        String contenido = raiz.path("choices").path(0).path("message").path("content").asString("");
        int inicio = contenido.indexOf('{');
        int fin = contenido.lastIndexOf('}');
        if (inicio < 0 || fin <= inicio) {
            return Optional.empty();
        }
        JsonNode clasificacion = JSON.readTree(contenido.substring(inicio, fin + 1));
        String gravedad = clasificacion.path("gravedad").asString("").trim().toUpperCase(Locale.ROOT)
                .replace("CRÍTICA", "CRITICA");
        try {
            return Optional.of(new SugerenciaGravedad(Novedad.Gravedad.valueOf(gravedad),
                    clasificacion.path("justificacion").asString(""), modelo));
        } catch (IllegalArgumentException e) {
            LOG.warn("El modelo devolvió una gravedad desconocida: {}", gravedad);
            return Optional.empty();
        }
    }
}
