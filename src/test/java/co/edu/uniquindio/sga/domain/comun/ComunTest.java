package co.edu.uniquindio.sga.domain.comun;

import co.edu.uniquindio.sga.domain.reserva.Estancia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static co.edu.uniquindio.sga.domain.Escenario.capturar;
import static co.edu.uniquindio.sga.domain.Escenario.noviembre;
import static org.assertj.core.api.Assertions.assertThat;

class ComunTest {

    @Nested
    @DisplayName("RN-03 y definición 3.1: estancia [entrada, salida)")
    class EstanciaYNoches {

        @Test
        @DisplayName("Del 10 al 12 son dos noches: la del 10 y la del 11")
        void delDiezAlDoceSonDosNoches() {
            // Arrange
            Estancia estancia = noviembre(10, 12);

            // Act
            List<LocalDate> noches = estancia.listaNoches();

            // Assert
            assertThat(noches).containsExactly(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 11));
            assertThat(estancia.noches()).isEqualTo(2);
        }

        @Test
        @DisplayName("RN-03 violada: salida igual a la entrada")
        void salidaIgualALaEntradaSeRechaza() {
            // Arrange
            LocalDate dia = LocalDate.of(2026, 11, 10);

            // Act
            ExcepcionNegocio error = capturar(() -> new Estancia(dia, dia));

            // Assert
            assertThat(error.getCodigo()).isEqualTo(CodigoError.ESTANCIA_INVALIDA);
        }

        @Test
        @DisplayName("RN-03 violada: salida anterior a la entrada")
        void salidaAnteriorSeRechaza() {
            // Arrange
            LocalDate entrada = LocalDate.of(2026, 11, 12);
            LocalDate salida = LocalDate.of(2026, 11, 10);

            // Act
            ExcepcionNegocio error = capturar(() -> new Estancia(entrada, salida));

            // Assert
            assertThat(error.getCodigo()).isEqualTo(CodigoError.ESTANCIA_INVALIDA);
        }

        @Test
        @DisplayName("RN-01: 10–12 y 11–13 comparten la noche del 11")
        void estanciasQueCompartenNocheSeSolapan() {
            // Arrange
            Estancia primera = noviembre(10, 12);
            Estancia segunda = noviembre(11, 13);

            // Act
            boolean seSolapan = primera.seSolapaCon(segunda);

            // Assert
            assertThat(seSolapan).isTrue();
            assertThat(segunda.seSolapaCon(primera)).as("el solapamiento es simétrico").isTrue();
        }

        @Test
        @DisplayName("RN-01: 10–12 y 12–14 no se solapan (la noche de salida no se ocupa)")
        void estanciasContiguasNoSeSolapan() {
            // Arrange
            Estancia primera = noviembre(10, 12);
            Estancia siguiente = noviembre(12, 14);

            // Act
            boolean seSolapan = primera.seSolapaCon(siguiente);

            // Assert
            assertThat(seSolapan).isFalse();
        }

        @Test
        @DisplayName("Una estancia contenida en otra se solapa")
        void estanciaContenidaSeSolapa() {
            // Arrange
            Estancia larga = noviembre(10, 20);
            Estancia contenida = noviembre(12, 13);

            // Act
            boolean seSolapan = larga.seSolapaCon(contenida);

            // Assert
            assertThat(seSolapan).isTrue();
        }
    }

    @Nested
    @DisplayName("Definición 3.4: dinero")
    class DineroExacto {

        @Test
        @DisplayName("Redondea al peso más cercano solo cuando se pide")
        void redondeaAlFinal() {
            // Arrange
            Dinero valor = Dinero.de(100_005).porcentaje(BigDecimal.TEN);

            // Act
            Dinero redondeado = valor.redondear();

            // Assert
            assertThat(valor.valor()).isEqualByComparingTo("10000.5");
            assertThat(redondeado).isEqualTo(Dinero.de(10_001));
        }

        @Test
        @DisplayName("Redondear al final da distinto que redondear en cada paso")
        void redondearAlFinalEvitaAcumularErrores() {
            // Arrange
            Dinero parte = Dinero.de(5).porcentaje(BigDecimal.TEN); // 0,5

            // Act
            Dinero sumaRedondeadaAlFinal = parte.mas(parte).mas(parte).redondear();
            Dinero sumaRedondeandoCadaPaso = parte.redondear().mas(parte.redondear()).mas(parte.redondear());

            // Assert
            assertThat(sumaRedondeadaAlFinal).isEqualTo(Dinero.de(2));
            assertThat(sumaRedondeandoCadaPaso).isEqualTo(Dinero.de(3));
        }

        @Test
        @DisplayName("La igualdad no depende de la escala decimal")
        void igualdadIndependienteDeEscala() {
            // Arrange
            Dinero conDecimales = Dinero.de(new BigDecimal("100.00"));

            // Act
            boolean iguales = conDecimales.equals(Dinero.de(100));

            // Assert
            assertThat(iguales).isTrue();
        }

        @Test
        @DisplayName("Un valor ausente no se acepta como dinero")
        void valorAusenteSeRechaza() {
            // Arrange
            BigDecimal ausente = null;

            // Act
            ExcepcionNegocio error = capturar(() -> Dinero.de(ausente));

            // Assert
            assertThat(error.getCodigo()).isEqualTo(CodigoError.DINERO_INVALIDO);
        }
    }
}
