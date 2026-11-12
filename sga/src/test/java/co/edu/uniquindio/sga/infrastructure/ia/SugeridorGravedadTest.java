package co.edu.uniquindio.sga.infrastructure.ia;

import co.edu.uniquindio.sga.domain.apartamento.Novedad;
import co.edu.uniquindio.sga.domain.apartamento.SugeridorGravedad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** Las pruebas nunca llaman a un modelo real (I-08): usan la alternativa local y respuestas simuladas. */
class SugeridorGravedadTest {

    @Test
    @DisplayName("El sugeridor simulado clasifica por palabras clave, sin conexión")
    void simulado() {
        SugeridorGravedadSimulado simulado = new SugeridorGravedadSimulado();

        assertThat(simulado.sugerir("Huele a GAS en la cocina").orElseThrow().gravedad()).isEqualTo(Novedad.Gravedad.CRITICA);
        assertThat(simulado.sugerir("La ducha no calienta, el calentador está dañado").orElseThrow().gravedad())
                .isEqualTo(Novedad.Gravedad.ALTA);
        assertThat(simulado.sugerir("Falta un bombillo en el balcón").orElseThrow().gravedad())
                .isEqualTo(Novedad.Gravedad.MEDIA);
        assertThat(simulado.sugerir("El cuadro de la sala está torcido").orElseThrow().gravedad())
                .isEqualTo(Novedad.Gravedad.BAJA);
    }

    @Test
    @DisplayName("Interpreta la respuesta de una API compatible aunque traiga texto alrededor del JSON")
    void interpretaRespuesta() {
        SugeridorGravedadOpenAi modelo = new SugeridorGravedadOpenAi("http://localhost:9", "modelo-prueba", null, 100);
        String respuesta = """
                {"choices":[{"message":{"role":"assistant","content":"Claro: {\\"gravedad\\":\\"alta\\",\\"justificacion\\":\\"Fuga en el baño\\"}"}}]}""";

        Optional<SugeridorGravedad.SugerenciaGravedad> sugerencia = modelo.interpretar(respuesta);

        assertThat(sugerencia).hasValueSatisfying(s -> {
            assertThat(s.gravedad()).isEqualTo(Novedad.Gravedad.ALTA);
            assertThat(s.justificacion()).isEqualTo("Fuga en el baño");
            assertThat(s.proveedor()).isEqualTo("modelo-prueba");
        });
        assertThat(modelo.interpretar("""
                {"choices":[{"message":{"content":"No sé"}}]}""")).isEmpty();
        assertThat(modelo.interpretar("""
                {"choices":[{"message":{"content":"{\\"gravedad\\":\\"URGENTE\\"}"}}]}""")).isEmpty();
    }

    @Test
    @DisplayName("Si el modelo no está disponible, no hay sugerencia pero tampoco error")
    void modeloCaido() {
        SugeridorGravedadOpenAi modelo = new SugeridorGravedadOpenAi("http://localhost:9", "modelo-prueba", null, 500);

        assertThat(modelo.sugerir("Gotera en el techo")).isEmpty();
    }

    @Test
    @DisplayName("I-03: si el modelo tarda más del tiempo máximo, se descarta la sugerencia")
    void tiempoLimite() {
        SugeridorGravedad lento = descripcion -> {
            try {
                Thread.sleep(Duration.ofSeconds(5));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return Optional.of(new SugeridorGravedad.SugerenciaGravedad(Novedad.Gravedad.ALTA, "tarde", "lento"));
        };
        long inicio = System.nanoTime();

        Optional<SugeridorGravedad.SugerenciaGravedad> resultado = new SugeridorConTiempoLimite(lento, 200).sugerir("x");

        assertThat(resultado).isEmpty();
        assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofSeconds(2));
    }
}
