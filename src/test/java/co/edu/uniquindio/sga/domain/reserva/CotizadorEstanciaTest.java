package co.edu.uniquindio.sga.domain.reserva;

import co.edu.uniquindio.sga.domain.comun.Dinero;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static co.edu.uniquindio.sga.domain.Escenario.*;
import static org.assertj.core.api.Assertions.assertThat;

class CotizadorEstanciaTest {

    private final CotizadorEstancia cotizador = new CotizadorEstancia();

    /** El grupo siempre incluye al titular (adulto) más los acompañantes indicados. */
    private static List<Ocupante> grupo(Ocupante... acompanantes) {
        List<Ocupante> ocupantes = new ArrayList<>();
        ocupantes.add(titular().comoOcupante());
        ocupantes.addAll(List.of(acompanantes));
        return ocupantes;
    }

    @Test
    @DisplayName("RN-05 y RN-06: el niño menor al umbral no genera cargo")
    void ninoNoFacturable() {
        // Arrange
        Ocupante ninoDeCuatro = ocupanteNacidoEl(LocalDate.of(2022, 6, 1));
        List<Ocupante> ocupantes = grupo(adulto("Pedro"), ninoDeCuatro);

        // Act
        CotizacionEstancia cotizacion = cotizador.cotizar("BH-201", noviembre(10, 12), ocupantes, 0, contexto());

        // Assert
        assertThat(cotizacion.detalle()).allSatisfy(n -> assertThat(n.ocupantesFacturables()).isEqualTo(2));
        assertThat(cotizacion.valorAlojamiento()).isEqualTo(Dinero.de(340_000));
    }

    @Test
    @DisplayName("RN-05: una estancia que cruza temporadas se liquida noche por noche")
    void estanciaQueCruzaTemporadas() {
        // Arrange
        Estancia cruzaTemporadas = estancia(LocalDate.of(2026, 12, 13), LocalDate.of(2026, 12, 17));
        List<Ocupante> ocupantes = grupo(adulto("Pedro"));

        // Act
        CotizacionEstancia cotizacion = cotizador.cotizar("BH-101", cruzaTemporadas, ocupantes, 0, contexto());

        // Assert
        assertThat(cotizacion.detalle()).extracting(DetalleNoche::temporada)
                .containsExactly(BASE, BASE, FIN_DE_ANIO, FIN_DE_ANIO);
        assertThat(cotizacion.detalle()).extracting(DetalleNoche::subtotal)
                .containsExactly(Dinero.de(190_000), Dinero.de(190_000), Dinero.de(280_000), Dinero.de(280_000));
        assertThat(cotizacion.valorAlojamiento()).isEqualTo(Dinero.de(940_000));
    }

    @Test
    @DisplayName("RN-06: quien cumple el umbral durante la estancia no cambia de condición")
    void cumpleAniosDuranteLaEstancia() {
        // Arrange
        Estancia estancia = estancia(LocalDate.of(2026, 11, 28), LocalDate.of(2026, 12, 2));
        Ocupante cumpleCincoAlDiaSiguiente = ocupanteNacidoEl(LocalDate.of(2021, 11, 29));

        // Act
        CotizacionEstancia cotizacion = cotizador.cotizar("BH-201", estancia, grupo(cumpleCincoAlDiaSiguiente), 0,
                contexto());

        // Assert
        assertThat(cotizacion.detalle()).allSatisfy(n -> assertThat(n.ocupantesFacturables()).isEqualTo(1));
        assertThat(cotizacion.valorAlojamiento()).isEqualTo(Dinero.de(340_000));
    }

    @Test
    @DisplayName("RN-06: quien alcanza el umbral justo el día de entrada es facturable")
    void cumpleUmbralElDiaDeEntrada() {
        // Arrange
        Estancia estancia = estancia(LocalDate.of(2026, 11, 28), LocalDate.of(2026, 12, 2));
        Ocupante cumpleCincoElDiaDeEntrada = ocupanteNacidoEl(LocalDate.of(2021, 11, 28));

        // Act
        CotizacionEstancia cotizacion = cotizador.cotizar("BH-201", estancia, grupo(cumpleCincoElDiaDeEntrada), 0,
                contexto());

        // Assert
        assertThat(cotizacion.valorAlojamiento()).isEqualTo(Dinero.de(680_000));
    }

    @Test
    @DisplayName("RP-02: 7 noches reciben 10 % de descuento sobre el alojamiento")
    void descuentoEstanciaLarga() {
        // Arrange
        Estancia sieteNoches = noviembre(1, 8);

        // Act
        CotizacionEstancia cotizacion = cotizador.cotizar("BH-201", sieteNoches, grupo(), 0, contexto());

        // Assert
        assertThat(cotizacion.subtotalNoches()).isEqualTo(Dinero.de(595_000));
        assertThat(cotizacion.descuento()).isEqualTo(Dinero.de(59_500));
        assertThat(cotizacion.valorAlojamiento()).isEqualTo(Dinero.de(535_500));
    }

    @Test
    @DisplayName("RP-02 no aplica: 6 noches no reciben descuento")
    void sinDescuentoEstanciaCorta() {
        // Arrange
        Estancia seisNoches = noviembre(1, 7);

        // Act
        CotizacionEstancia cotizacion = cotizador.cotizar("BH-201", seisNoches, grupo(), 0, contexto());

        // Assert
        assertThat(cotizacion.descuento()).isEqualTo(Dinero.CERO);
        assertThat(cotizacion.valorAlojamiento()).isEqualTo(Dinero.de(510_000));
    }

    @Test
    @DisplayName("RP-03: la mascota genera un cargo fijo por estancia")
    void cargoPorMascota() {
        // Arrange
        int unaMascota = 1;

        // Act
        CotizacionEstancia cotizacion = cotizador.cotizar("BH-202", noviembre(10, 12), grupo(), unaMascota,
                contexto());

        // Assert
        assertThat(cotizacion.valorMascotas()).isEqualTo(Dinero.de(40_000));
        assertThat(cotizacion.valorTotal()).isEqualTo(Dinero.de(170_000 + 40_000));
    }
}
