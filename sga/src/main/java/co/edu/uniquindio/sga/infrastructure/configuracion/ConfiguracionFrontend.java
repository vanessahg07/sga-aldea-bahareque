package co.edu.uniquindio.sga.infrastructure.configuracion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * En el despliegue, el mismo servidor entrega el frontend compilado ({@code sga.frontend.directorio}).
 * Las rutas del frontend que no son archivos (por ejemplo /reservas/123) devuelven index.html
 * para que React Router las resuelva; las rutas de la API nunca se confunden con el frontend.
 */
@Configuration
public class ConfiguracionFrontend implements WebMvcConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(ConfiguracionFrontend.class);

    private final String directorio;

    public ConfiguracionFrontend(PropiedadesSga propiedades) {
        this.directorio = propiedades.frontend().directorio();
    }

    private boolean hayFrontend() {
        return directorio != null && !directorio.isBlank()
                && Files.isRegularFile(Path.of(directorio).toAbsolutePath().normalize().resolve("index.html"));
    }

    /** La raíz no pasa por el manejador de recursos: se reenvía explícitamente a index.html. */
    @Override
    public void addViewControllers(ViewControllerRegistry registro) {
        if (hayFrontend()) {
            registro.addViewController("/").setViewName("forward:/index.html");
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        if (directorio == null || directorio.isBlank()) {
            return;
        }
        Path carpeta = Path.of(directorio).toAbsolutePath().normalize();
        if (!hayFrontend()) {
            LOG.warn("sga.frontend.directorio={} no contiene index.html; el frontend no se servirá", carpeta);
            return;
        }
        LOG.info("Sirviendo el frontend desde {}", carpeta);
        String ubicacion = carpeta.toUri().toString();
        registro.addResourceHandler("/**")
                .addResourceLocations(ubicacion.endsWith("/") ? ubicacion : ubicacion + "/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String ruta, Resource ubicacionBase) throws IOException {
                        if (ruta.startsWith("api/") || ruta.startsWith("h2-console")) {
                            return null;
                        }
                        Resource recurso = ubicacionBase.createRelative(ruta);
                        if (recurso.exists() && recurso.isReadable()) {
                            return recurso;
                        }
                        return ruta.contains(".") ? null : ubicacionBase.createRelative("index.html");
                    }
                });
    }
}
