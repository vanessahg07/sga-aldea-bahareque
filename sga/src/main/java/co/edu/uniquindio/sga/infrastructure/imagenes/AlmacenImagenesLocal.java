package co.edu.uniquindio.sga.infrastructure.imagenes;

import co.edu.uniquindio.sga.domain.apartamento.AlmacenImagenes;
import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Alternativa local del almacén de imágenes (I-02): guarda los archivos en una carpeta del
 * servidor y los publica bajo {@value #RUTA_PUBLICA}. Funciona sin conexión ni credenciales.
 */
public class AlmacenImagenesLocal implements AlmacenImagenes {

    public static final String RUTA_PUBLICA = "/archivos/imagenes/";

    private final Path directorio;

    public AlmacenImagenesLocal(Path directorio) {
        this.directorio = directorio.toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.directorio);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear la carpeta de imágenes " + this.directorio, e);
        }
    }

    public Path directorio() {
        return directorio;
    }

    @Override
    public String guardar(Archivo archivo) {
        String nombre = UUID.randomUUID() + "." + archivo.extension();
        try {
            Files.write(directorio.resolve(nombre), archivo.contenido());
            return RUTA_PUBLICA + nombre;
        } catch (IOException e) {
            throw new ExcepcionNegocio(CodigoError.SERVICIO_EXTERNO_NO_DISPONIBLE, "no se pudo guardar la imagen");
        }
    }
}
