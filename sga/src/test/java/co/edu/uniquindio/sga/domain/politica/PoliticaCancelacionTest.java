package co.edu.uniquindio.sga.domain.politica;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.Dinero;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static co.edu.uniquindio.sga.domain.Escenario.capturar;
import static co.edu.uniquindio.sga.domain.Escenario.politicaV1;
import static org.assertj.core.api.Assertions.assertThat;

class PoliticaCancelacionTest {

    private static final LocalDate ENTRADA = LocalDate.of(2026, 11, 20);
    private static final LocalDateTime VIGENTE_DESDE = LocalDateTime.of(2026, 1, 1, 0, 0);

    @ParameterizedTest(name = "{0} días de antelación → retención {1}")
    @CsvSource({"30, 0", "15, 0", "14, 30000", "7, 30000", "6, 60000", "0, 60000"})
    @DisplayName("RN-13: aplica el tramo según los días de antelación (valor 100.000)")
    void tramosDeAntelacion(int dias, long retencionEsperada) {
        // Arrange
        PoliticaCancelacion politica = politicaV1();
        LocalDate fechaCancelacion = ENTRADA.minusDays(dias);

        // Act
        Dinero retencion = politica.retencionPorCancelacion(Dinero.de(100_000), fechaCancelacion, ENTRADA);

        // Assert
        assertThat(retencion).isEqualTo(Dinero.de(retencionEsperada));
    }

    @Test
    @DisplayName("Se exigen al menos dos tramos")
    void minimoDosTramos() {
        // Arrange
        List<TramoCancelacion> unSoloTramo = List.of(new TramoCancelacion(0, BigDecimal.TEN));

        // Act
        ExcepcionNegocio error = capturar(
                () -> new PoliticaCancelacion(1, VIGENTE_DESDE, unSoloTramo, BigDecimal.TEN));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.POLITICA_INVALIDA);
    }

    @Test
    @DisplayName("Debe existir un tramo que cubra la antelación de 0 días")
    void debeCubrirCeroDias() {
        // Arrange
        List<TramoCancelacion> sinTramoDeCeroDias = List.of(
                new TramoCancelacion(15, BigDecimal.ZERO), new TramoCancelacion(7, BigDecimal.TEN));

        // Act
        ExcepcionNegocio error = capturar(
                () -> new PoliticaCancelacion(1, VIGENTE_DESDE, sinTramoDeCeroDias, BigDecimal.TEN));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.POLITICA_INVALIDA);
    }

    @Test
    @DisplayName("Actualizar la política crea una versión nueva y conserva la anterior")
    void versionado() {
        // Arrange
        PoliticaCancelacion v1 = politicaV1();
        List<TramoCancelacion> tramosNuevos = List.of(
                new TramoCancelacion(30, BigDecimal.ZERO), new TramoCancelacion(0, BigDecimal.valueOf(50)));

        // Act
        PoliticaCancelacion v2 = v1.nuevaVersion(LocalDateTime.of(2026, 10, 15, 0, 0), tramosNuevos,
                BigDecimal.valueOf(50));

        // Assert
        assertThat(v2.version()).isEqualTo(2);
        assertThat(v1.version()).isEqualTo(1);
        assertThat(v1.tramos()).hasSize(3);
    }

    @Test
    @DisplayName("RN-13: la retención por no-show redondea al peso")
    void retencionNoShow() {
        // Arrange
        PoliticaCancelacion politica = politicaV1();

        // Act
        Dinero retencion = politica.retencionPorNoShow(Dinero.de(100_001));

        // Assert
        assertThat(retencion).isEqualTo(Dinero.de(30_000));
    }
}
