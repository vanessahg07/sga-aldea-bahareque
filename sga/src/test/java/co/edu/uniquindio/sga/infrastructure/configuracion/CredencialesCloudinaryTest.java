package co.edu.uniquindio.sga.infrastructure.configuracion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Pruebas de la lectura de credenciales de Cloudinary desde CLOUDINARY_URL o desde variables separadas. */
class CredencialesCloudinaryTest {

    @Test
    @DisplayName("CLOUDINARY_URL completa la nube, la clave y el secreto")
    void desdeUrl() {
        // Arrange y Act
        PropiedadesSga.Cloudinary c = new PropiedadesSga.Cloudinary(null, null, null, "sga",
                "cloudinary://123456:secreto-de-prueba@mi-nube");

        // Assert
        assertThat(c.nube()).isEqualTo("mi-nube");
        assertThat(c.claveApi()).isEqualTo("123456");
        assertThat(c.secretoApi()).isEqualTo("secreto-de-prueba");
    }

    @Test
    @DisplayName("Las variables separadas tienen prioridad sobre CLOUDINARY_URL")
    void prioridadDeLasVariablesSeparadas() {
        // Arrange y Act
        PropiedadesSga.Cloudinary c = new PropiedadesSga.Cloudinary("otra-nube", "999", "otro-secreto", "sga",
                "cloudinary://123456:secreto-de-prueba@mi-nube");

        // Assert
        assertThat(c.nube()).isEqualTo("otra-nube");
        assertThat(c.claveApi()).isEqualTo("999");
        assertThat(c.secretoApi()).isEqualTo("otro-secreto");
    }

    @Test
    @DisplayName("Sin URL ni variables, las credenciales quedan vacías")
    void sinCredenciales() {
        // Arrange y Act
        PropiedadesSga.Cloudinary c = new PropiedadesSga.Cloudinary(null, null, null, "sga", "");

        // Assert
        assertThat(c.nube()).isNull();
        assertThat(c.secretoApi()).isNull();
    }
}
