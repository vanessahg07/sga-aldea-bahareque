package co.edu.uniquindio.sga.domain.alojamiento;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.Dinero;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;

import static co.edu.uniquindio.sga.domain.Escenario.capturar;
import static co.edu.uniquindio.sga.domain.Escenario.parametrosConPreparacion;
import static org.assertj.core.api.Assertions.assertThat;

class ParametrosAlojamientoTest {

    @Test
    @DisplayName("Ventana 11:00–15:00 (4 h): 3 h de preparación admite entrada el mismo día")
    void preparacionDentroDeLaVentana() {
        // Arrange
        ParametrosAlojamiento parametros = parametrosConPreparacion(3);

        // Act
        boolean admite = parametros.admiteEntradaMismoDiaDeSalida();

        // Assert
        assertThat(admite).isTrue();
    }

    @Test
    @DisplayName("Preparación igual a la ventana todavía admite entrada el mismo día")
    void preparacionIgualALaVentana() {
        // Arrange
        ParametrosAlojamiento parametros = parametrosConPreparacion(4);

        // Act
        boolean admite = parametros.admiteEntradaMismoDiaDeSalida();

        // Assert
        assertThat(admite).isTrue();
    }

    @Test
    @DisplayName("Preparación que excede la ventana impide entrada el mismo día")
    void preparacionExcedeLaVentana() {
        // Arrange
        ParametrosAlojamiento parametros = parametrosConPreparacion(5);

        // Act
        boolean admite = parametros.admiteEntradaMismoDiaDeSalida();

        // Assert
        assertThat(admite).isFalse();
    }

    @Test
    @DisplayName("Un anticipo mayor a 100 % se rechaza")
    void anticipoInvalido() {
        // Arrange
        BigDecimal anticipoDeMas = BigDecimal.valueOf(120);

        // Act
        ExcepcionNegocio error = capturar(() -> new ParametrosAlojamiento(5, LocalTime.of(15, 0),
                LocalTime.of(11, 0), 3, 24, LocalTime.of(22, 0), anticipoDeMas, 1,
                Dinero.de(40_000), 7, BigDecimal.TEN, 2));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.PARAMETROS_INVALIDOS);
    }
}
