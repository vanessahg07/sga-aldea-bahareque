package co.edu.uniquindio.sga.infrastructure.seguridad;

import co.edu.uniquindio.sga.infrastructure.configuracion.PropiedadesSga;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de la clave de firma de los JWT: longitud mínima y secreto obligatorio en el despliegue. */
class ClaveJwtTest {

    private static PropiedadesSga propiedades(String secreto, boolean obligatorio) {
        return new PropiedadesSga(null, new PropiedadesSga.Jwt(secreto, obligatorio, 60, "sga-aldea-bahareque"),
                null, null, null, null, null, null, null);
    }

    @Test
    @DisplayName("En desarrollo, sin secreto, se genera una clave temporal de 256 bits")
    void claveTemporalEnDesarrollo() {
        // Arrange
        PropiedadesSga config = propiedades(null, false);

        // Act
        ClaveJwt clave = new ClaveJwt(config);

        // Assert
        assertThat(clave.clave().getEncoded()).hasSize(32);
    }

    @Test
    @DisplayName("En el despliegue, sin secreto, la aplicación no arranca")
    void secretoObligatorioEnDespliegue() {
        // Arrange
        PropiedadesSga config = propiedades("", true);

        // Act y Assert
        assertThatThrownBy(() -> new ClaveJwt(config))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SGA_JWT_SECRETO");
    }

    @Test
    @DisplayName("Un secreto de menos de 32 caracteres se rechaza")
    void secretoCorto() {
        // Arrange
        PropiedadesSga config = propiedades("corto", true);

        // Act y Assert
        assertThatThrownBy(() -> new ClaveJwt(config)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Un secreto válido se usa tal cual para firmar con HMAC-SHA256")
    void secretoValido() {
        // Arrange
        String secreto = "una-clave-de-prueba-con-mas-de-32-caracteres";

        // Act
        ClaveJwt clave = new ClaveJwt(propiedades(secreto, true));

        // Assert
        assertThat(clave.clave().getAlgorithm()).isEqualTo("HmacSHA256");
        assertThat(clave.clave().getEncoded()).hasSize(secreto.length());
    }
}
