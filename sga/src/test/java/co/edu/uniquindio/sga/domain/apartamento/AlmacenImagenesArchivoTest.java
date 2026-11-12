package co.edu.uniquindio.sga.domain.apartamento;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static co.edu.uniquindio.sga.domain.Escenario.violaRegla;
import static org.assertj.core.api.Assertions.assertThat;

class AlmacenImagenesArchivoTest {

    @Test
    @DisplayName("Acepta JPG, PNG y WEBP de hasta 5 MB")
    void aceptaFormatosWeb() {
        assertThat(new AlmacenImagenes.Archivo("foto.png", "image/png", new byte[10]).extension()).isEqualTo("png");
        assertThat(new AlmacenImagenes.Archivo(null, "image/jpeg", new byte[10]).extension()).isEqualTo("jpg");
        assertThat(new AlmacenImagenes.Archivo("x", "image/webp", new byte[(int) AlmacenImagenes.Archivo.TAMANO_MAXIMO])
                .extension()).isEqualTo("webp");
    }

    @Test
    @DisplayName("Rechaza otros formatos, archivos vacíos y archivos de más de 5 MB")
    void rechazaInvalidas() {
        violaRegla(CodigoError.IMAGEN_INVALIDA, () -> new AlmacenImagenes.Archivo("a.gif", "image/gif", new byte[10]));
        violaRegla(CodigoError.IMAGEN_INVALIDA, () -> new AlmacenImagenes.Archivo("a.png", "image/png", new byte[0]));
        violaRegla(CodigoError.IMAGEN_INVALIDA, () -> new AlmacenImagenes.Archivo("a.png", "image/png",
                new byte[(int) AlmacenImagenes.Archivo.TAMANO_MAXIMO + 1]));
    }
}
