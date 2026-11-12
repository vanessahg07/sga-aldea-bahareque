package co.edu.uniquindio.sga.infrastructure.rest.comun;

import co.edu.uniquindio.sga.domain.comun.Pagina;

import java.util.List;
import java.util.function.Function;

/** Página de resultados de la API: siempre de 10 elementos. */
public record PaginaRespuesta<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    public static <D, T> PaginaRespuesta<T> de(Pagina<D> pagina, Function<D, T> transformacion) {
        return new PaginaRespuesta<>(pagina.contenido().stream().map(transformacion).toList(), pagina.numero(),
                Pagina.TAMANO, pagina.totalElementos(), pagina.totalPaginas());
    }
}
