package co.edu.uniquindio.sga.domain.apartamento;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;

import java.util.Set;

/**
 * Puerto de almacenamiento de las imágenes de los apartamentos. Devuelve la URL pública
 * con la que se registra la imagen en el apartamento; el dominio no conoce el proveedor.
 */
public interface AlmacenImagenes {

    String guardar(Archivo archivo);

    /** Imagen recibida para almacenar. Solo se aceptan formatos web comunes y máximo 5 MB. */
    record Archivo(String nombre, String tipoContenido, byte[] contenido) {

        public static final long TAMANO_MAXIMO = 5L * 1024 * 1024;
        public static final Set<String> TIPOS = Set.of("image/jpeg", "image/png", "image/webp");

        public Archivo {
            ExcepcionNegocio.exigir(tipoContenido != null && TIPOS.contains(tipoContenido) && contenido != null
                    && contenido.length > 0 && contenido.length <= TAMANO_MAXIMO, CodigoError.IMAGEN_INVALIDA);
            nombre = nombre == null || nombre.isBlank() ? "imagen" : nombre;
        }

        public String extension() {
            return switch (tipoContenido) {
                case "image/png" -> "png";
                case "image/webp" -> "webp";
                default -> "jpg";
            };
        }
    }
}
