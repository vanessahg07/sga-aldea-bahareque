package co.edu.uniquindio.sga.domain.tarifa;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.Dinero;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import co.edu.uniquindio.sga.domain.comun.RangoFechas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static co.edu.uniquindio.sga.domain.Escenario.*;
import static org.assertj.core.api.Assertions.assertThat;

class TemporadasYTarifasTest {

    @Test
    @DisplayName("Una fecha dentro de un rango de temporada pertenece a esa temporada")
    void fechaDentroDeTemporada() {
        // Arrange
        CalendarioTemporadas calendario = calendario();

        // Act
        Temporada temporada = calendario.temporadaDe(LocalDate.of(2026, 12, 24));

        // Assert
        assertThat(temporada.nombre()).isEqualTo(FIN_DE_ANIO);
    }

    @Test
    @DisplayName("Toda fecha tiene temporada: las no asignadas caen en la base")
    void fechaNoAsignadaCaeEnBase() {
        // Arrange
        CalendarioTemporadas calendario = calendario();

        // Act
        Temporada temporada = calendario.temporadaDe(LocalDate.of(2026, 10, 20));

        // Assert
        assertThat(temporada.nombre()).isEqualTo(BASE);
    }

    @Test
    @DisplayName("El rango es [inicio, fin): la fecha final ya no pertenece a la temporada")
    void fechaFinalNoPerteneceALaTemporada() {
        // Arrange: FIN_DE_ANIO cubre [15-dic-2026, 16-ene-2027)
        CalendarioTemporadas calendario = calendario();

        // Act
        Temporada ultimoDia = calendario.temporadaDe(LocalDate.of(2027, 1, 15));
        Temporada diaSiguiente = calendario.temporadaDe(LocalDate.of(2027, 1, 16));

        // Assert
        assertThat(ultimoDia.nombre()).isEqualTo(FIN_DE_ANIO);
        assertThat(diaSiguiente.nombre()).isEqualTo(BASE);
    }

    @Test
    @DisplayName("Sin temporada base el calendario se rechaza")
    void sinTemporadaBase() {
        // Arrange
        List<Temporada> soloAlta = List.of(
                Temporada.de("ALTA", 1, new RangoFechas(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31))));

        // Act
        ExcepcionNegocio error = capturar(() -> new CalendarioTemporadas(soloAlta));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.SIN_TEMPORADA_BASE);
    }

    @Test
    @DisplayName("Dos temporadas base se rechazan")
    void dosTemporadasBase() {
        // Arrange
        List<Temporada> dosBases = List.of(Temporada.base("BASE", 1), Temporada.base("OTRA", 1));

        // Act
        ExcepcionNegocio error = capturar(() -> new CalendarioTemporadas(dosBases));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.SIN_TEMPORADA_BASE);
    }

    @Test
    @DisplayName("Las temporadas no pueden solaparse entre sí")
    void temporadasSolapadas() {
        // Arrange
        List<Temporada> solapadas = List.of(
                Temporada.base("BASE", 1),
                Temporada.de("A", 1, new RangoFechas(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 20))),
                Temporada.de("B", 1, new RangoFechas(LocalDate.of(2026, 12, 19), LocalDate.of(2027, 1, 5))));

        // Act
        ExcepcionNegocio error = capturar(() -> new CalendarioTemporadas(solapadas));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.TEMPORADAS_SOLAPADAS);
    }

    @Test
    @DisplayName("Temporadas contiguas (una termina donde empieza otra) son válidas")
    void temporadasContiguas() {
        // Arrange
        CalendarioTemporadas calendario = new CalendarioTemporadas(List.of(
                Temporada.base("BASE", 1),
                Temporada.de("A", 1, new RangoFechas(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 20))),
                Temporada.de("B", 1, new RangoFechas(LocalDate.of(2026, 12, 20), LocalDate.of(2027, 1, 5)))));

        // Act
        Temporada temporada = calendario.temporadaDe(LocalDate.of(2026, 12, 20));

        // Assert
        assertThat(temporada.nombre()).isEqualTo("B");
    }

    @Test
    @DisplayName("La tarifa vigente es la más reciente y se conserva el histórico")
    void historicoDeTarifas() {
        // Arrange
        TablaTarifas tarifas = tarifas();
        Tarifa nueva = new Tarifa("BH-201", BASE, Dinero.de(90_000), LocalDateTime.of(2026, 9, 1, 0, 0));

        // Act
        TablaTarifas actualizada = tarifas.conTarifa(nueva);

        // Assert
        assertThat(actualizada.valorPara("BH-201", BASE)).isEqualTo(Dinero.de(90_000));
        assertThat(actualizada.historico("BH-201").stream().filter(t -> t.nombreTemporada().equals(BASE)))
                .extracting(Tarifa::valor)
                .containsExactly(Dinero.de(85_000), Dinero.de(90_000));
        assertThat(tarifas.valorPara("BH-201", BASE)).as("la tabla original no cambia").isEqualTo(Dinero.de(85_000));
    }

    @Test
    @DisplayName("Una tarifa debe ser positiva")
    void tarifaNoPositiva() {
        // Arrange
        Dinero cero = Dinero.CERO;

        // Act
        ExcepcionNegocio error = capturar(
                () -> new Tarifa("BH-201", BASE, cero, LocalDateTime.of(2026, 1, 1, 0, 0)));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.TARIFA_INVALIDA);
    }

    @Test
    @DisplayName("Faltar tarifa en una temporada deja las tarifas incompletas")
    void tarifasIncompletas() {
        // Arrange
        TablaTarifas tarifas = new TablaTarifas(List.of(
                new Tarifa("BH-999", BASE, Dinero.de(80_000), LocalDateTime.of(2026, 1, 1, 0, 0))));

        // Act
        boolean completas = tarifas.tieneTarifasCompletas("BH-999", calendario());

        // Assert
        assertThat(completas).isFalse();
    }

    @Test
    @DisplayName("Pedir el valor de una temporada sin tarifa se rechaza")
    void valorSinTarifa() {
        // Arrange
        TablaTarifas tarifas = new TablaTarifas(List.of(
                new Tarifa("BH-999", BASE, Dinero.de(80_000), LocalDateTime.of(2026, 1, 1, 0, 0))));

        // Act
        ExcepcionNegocio error = capturar(() -> tarifas.valorPara("BH-999", FIN_DE_ANIO));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.TARIFAS_INCOMPLETAS);
    }
}
