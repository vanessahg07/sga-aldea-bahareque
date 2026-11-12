package co.edu.uniquindio.sga.infrastructure.ia;

import co.edu.uniquindio.sga.domain.apartamento.Novedad;
import co.edu.uniquindio.sga.domain.apartamento.SugeridorGravedad;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Alternativa local del modelo de IA (I-02): clasifica por palabras clave, sin conexión ni
 * credenciales. Permite demostrar el flujo y es la que usan las pruebas automáticas (I-08).
 */
public class SugeridorGravedadSimulado implements SugeridorGravedad {

    private record Regla(Novedad.Gravedad gravedad, List<String> palabras, String motivo) {
    }

    private static final List<Regla> REGLAS = List.of(
            new Regla(Novedad.Gravedad.CRITICA, List.of("incendio", "humo", "gas", "inundacion", "corto circuito",
                    "cortocircuito", "chispa", "sin agua", "sin luz", "herido", "cerradura rota", "no cierra la puerta"),
                    "riesgo para la seguridad o el apartamento no puede habitarse"),
            new Regla(Novedad.Gravedad.ALTA, List.of("fuga", "gotera", "no funciona", "no enciende", "roto", "rota",
                    "danado", "danada", "nevera", "calentador", "ducha", "sanitario", "inodoro", "jacuzzi"),
                    "afecta un servicio esencial del apartamento"),
            new Regla(Novedad.Gravedad.MEDIA, List.of("mancha", "falta", "faltante", "bombillo", "control",
                    "wifi", "internet", "televisor", "ruido", "rayado"),
                    "afecta la comodidad del huésped pero no impide la estancia"));

    @Override
    public Optional<SugerenciaGravedad> sugerir(String descripcion) {
        String texto = normalizar(descripcion);
        for (Regla regla : REGLAS) {
            Optional<String> palabra = regla.palabras().stream().filter(texto::contains).findFirst();
            if (palabra.isPresent()) {
                return Optional.of(new SugerenciaGravedad(regla.gravedad(),
                        "Menciona \"" + palabra.get() + "\": " + regla.motivo() + ".", "simulado (reglas locales)"));
            }
        }
        return Optional.of(new SugerenciaGravedad(Novedad.Gravedad.BAJA,
                "No menciona daños ni fallas de servicios: parece un detalle menor.", "simulado (reglas locales)"));
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
