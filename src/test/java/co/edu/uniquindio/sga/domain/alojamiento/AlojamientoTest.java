package co.edu.uniquindio.sga.domain.alojamiento;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.Dinero;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import co.edu.uniquindio.sga.domain.folio.MedioPago;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static co.edu.uniquindio.sga.domain.Escenario.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class AlojamientoTest {

    private Alojamiento alojamiento() {
        return Alojamiento.crear("ALDEA-BAHAREQUE", "Aldea Bahareque", "Apartamentos", "Filandia", "Calle 5",
                new BigDecimal("4.6746"), new BigDecimal("-75.6582"), List.of("Silencio de 22:00 a 7:00"),
                List.of(new ServicioAdicional("DESAYUNO", "Desayuno típico", "", true, Dinero.de(25_000),
                        "por persona por día")),
                Set.of(EFECTIVO, TRANSFERENCIA), parametros());
    }

    @Test
    @DisplayName("Un medio de pago configurado se acepta")
    void medioDePagoAceptado() {
        // Arrange
        Alojamiento alojamiento = alojamiento();

        // Act
        Throwable error = catchThrowable(() -> alojamiento.validarMedioPago(EFECTIVO));

        // Assert
        assertThat(error).isNull();
    }

    @Test
    @DisplayName("Un medio de pago no configurado se rechaza")
    void medioDePagoNoAceptado() {
        // Arrange
        Alojamiento alojamiento = alojamiento();
        MedioPago criptomoneda = new MedioPago("CRIPTOMONEDA");

        // Act
        ExcepcionNegocio error = capturar(() -> alojamiento.validarMedioPago(criptomoneda));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.MEDIO_PAGO_NO_ACEPTADO);
    }

    @Test
    @DisplayName("L-18: se exigen al menos dos medios de pago")
    void minimoDosMediosDePago() {
        // Arrange
        Alojamiento alojamiento = alojamiento();

        // Act
        ExcepcionNegocio error = capturar(() -> alojamiento.definirMediosPago(Set.of(EFECTIVO)));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.ALOJAMIENTO_INVALIDO);
    }

    @Test
    @DisplayName("El cargo de un servicio adicional es su valor por la cantidad")
    void cargoDeServicio() {
        // Arrange
        ServicioAdicional desayuno = alojamiento().servicio("desayuno");

        // Act
        Dinero cargo = desayuno.valorPor(3);

        // Assert
        assertThat(cargo).isEqualTo(Dinero.de(75_000));
    }

    @Test
    @DisplayName("Un servicio que el alojamiento no ofrece no se encuentra")
    void servicioInexistente() {
        // Arrange
        Alojamiento alojamiento = alojamiento();

        // Act
        ExcepcionNegocio error = capturar(() -> alojamiento.servicio("SPA"));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.SERVICIO_NO_ENCONTRADO);
    }

    @Test
    @DisplayName("Un servicio con cargo necesita valor positivo")
    void servicioConCargoSinValor() {
        // Arrange
        Dinero sinValor = Dinero.CERO;

        // Act
        ExcepcionNegocio error = capturar(
                () -> new ServicioAdicional("TOUR", "Tour", "", true, sinValor, "por persona"));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.ALOJAMIENTO_INVALIDO);
    }

    @Test
    @DisplayName("Dos alojamientos con el mismo código son el mismo aunque cambien sus datos")
    void identidadPorCodigo() {
        // Arrange
        Alojamiento original = alojamiento();
        Alojamiento modificado = alojamiento();
        modificado.cambiarParametros(parametrosConPreparacion(2));

        // Act
        boolean mismoAlojamiento = original.equals(modificado);

        // Assert
        assertThat(mismoAlojamiento).isTrue();
        assertThat(original.hashCode()).isEqualTo(modificado.hashCode());
    }
}
