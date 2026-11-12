package co.edu.uniquindio.sga.infrastructure.imagenes;

import co.edu.uniquindio.sga.domain.apartamento.AlmacenImagenes;
import co.edu.uniquindio.sga.infrastructure.configuracion.PropiedadesSga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.time.Clock;

/**
 * Elige el almacén de imágenes por configuración ({@code sga.imagenes.proveedor}) y publica
 * la carpeta del almacén local.
 */
@Configuration
public class ConfiguracionImagenes implements WebMvcConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(ConfiguracionImagenes.class);

    private final PropiedadesSga.Imagenes imagenes;

    public ConfiguracionImagenes(PropiedadesSga propiedades) {
        this.imagenes = propiedades.imagenes();
    }

    @Bean
    public AlmacenImagenes almacenImagenes() {
        return switch (imagenes.proveedor()) {
            case "cloudinary" -> {
                LOG.info("Imágenes en Cloudinary (espera máxima {} ms)", imagenes.timeoutMs());
                yield new AlmacenImagenesCloudinary(imagenes.cloudinary(), imagenes.timeoutMs(), Clock.systemUTC());
            }
            case "local" -> {
                AlmacenImagenesLocal local = new AlmacenImagenesLocal(Path.of(imagenes.directorio()));
                LOG.info("Imágenes en la carpeta local {}", local.directorio());
                yield local;
            }
            default -> throw new IllegalStateException("Proveedor de imágenes desconocido: " + imagenes.proveedor()
                    + " (use 'local' o 'cloudinary')");
        };
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        String carpeta = Path.of(imagenes.directorio()).toAbsolutePath().normalize().toUri().toString();
        registro.addResourceHandler(AlmacenImagenesLocal.RUTA_PUBLICA + "**")
                .addResourceLocations(carpeta.endsWith("/") ? carpeta : carpeta + "/");
    }
}
