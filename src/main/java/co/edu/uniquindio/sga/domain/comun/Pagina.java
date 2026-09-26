package co.edu.uniquindio.sga.domain.comun;

import java.util.List;
import java.util.function.Function;

/**
 * Página de resultados. Todos los listados del sistema usan páginas de
 * {@value #TAMANO} elementos.
 *
 * @param numero número de página, empezando en 0
 */
public record Pagina<T>(List<T> contenido, int numero, long totalElementos) {

    public static final int TAMANO = 10;

    public Pagina {
        contenido = List.copyOf(contenido);
    }

    public int totalPaginas() {
        return (int) Math.ceil((double) totalElementos / TAMANO);
    }

    public <R> Pagina<R> map(Function<T, R> transformacion) {
        return new Pagina<>(contenido.stream().map(transformacion).toList(), numero, totalElementos);
    }

    /** Pagina en memoria una lista completa. */
    public static <T> Pagina<T> de(List<T> todos, int numero) {
        int desde = Math.min(Math.max(numero, 0) * TAMANO, todos.size());
        int hasta = Math.min(desde + TAMANO, todos.size());
        return new Pagina<>(todos.subList(desde, hasta), Math.max(numero, 0), todos.size());
    }
}
