package co.edu.uniquindio.sga.domain.reserva;

import co.edu.uniquindio.sga.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga.domain.apartamento.Bloqueo;
import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.Dinero;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import co.edu.uniquindio.sga.domain.comun.RangoFechas;
import co.edu.uniquindio.sga.domain.folio.TipoCargo;
import co.edu.uniquindio.sga.domain.tarifa.TablaTarifas;
import co.edu.uniquindio.sga.domain.tarifa.Tarifa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static co.edu.uniquindio.sga.domain.Escenario.*;
import static org.assertj.core.api.Assertions.assertThat;

class CreacionReservaTest {

    private final GestorReservas gestor = new GestorReservas();

    @Test
    @DisplayName("La reserva nace PENDIENTE, con valor congelado y folio abierto con el cargo de alojamiento")
    void reservaNaceConFolioAbierto() {
        // Arrange
        SolicitudReserva solicitud = solicitud(noviembre(10, 12), adulto("Pedro"));
        OcupacionApartamento cafetalLibre = libre(apartamento("BH-201"));

        // Act
        Reserva reserva = gestor.crear(solicitud, cafetalLibre, contexto(), AHORA);

        // Assert
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.PENDIENTE);
        assertThat(reserva.getPolitica().version()).isEqualTo(1);
        assertThat(reserva.getFolio().isCerrado()).isFalse();
        assertThat(reserva.getFolio().getCargos()).singleElement()
                .satisfies(c -> {
                    assertThat(c.tipo()).isEqualTo(TipoCargo.ALOJAMIENTO);
                    assertThat(c.valor()).isEqualTo(Dinero.de(340_000));
                });
        assertThat(reserva.getFolio().saldo()).isEqualTo(Dinero.de(340_000));
    }

    @Test
    @DisplayName("Dos reservas son la misma si tienen el mismo identificador")
    void identidadPorId() {
        // Arrange
        Reserva reserva = reservaPendiente(apartamento("BH-201"), noviembre(10, 12));
        Reserva reconstruida = Reserva.reconstituir(reserva.getId(), solicitud(noviembre(10, 12)), "BH-201",
                EstadoReserva.CONFIRMADA, AHORA, reserva.getCotizacion(), reserva.getPolitica(), reserva.getFolio());

        // Act
        boolean mismaReserva = reserva.equals(reconstruida);

        // Assert
        assertThat(mismaReserva).isTrue();
        assertThat(reserva).isNotEqualTo(reservaPendiente(apartamento("BH-201"), noviembre(10, 12)));
    }

    @Nested
    @DisplayName("RN-01: sin reservas activas solapadas")
    class Solapamiento {

        @Test
        @DisplayName("Violada: noches compartidas con una reserva activa")
        void rechazaSolapamiento() {
            // Arrange
            Apartamento apto = apartamento("BH-201");
            Reserva existente = reservaPendiente(apto, noviembre(10, 12));

            // Act
            ExcepcionNegocio error = capturar(() -> gestor.crear(solicitud(noviembre(11, 13)),
                    ocupacion(apto, List.of(existente)), contexto(), AHORA));

            // Assert
            assertThat(error.getCodigo()).isEqualTo(CodigoError.NOCHES_NO_DISPONIBLES);
        }

        @Test
        @DisplayName("Violada sin importar el canal: una reserva externa choca con una directa")
        void solapamientoEntreCanales() {
            // Arrange
            Apartamento apto = apartamento("BH-201");
            Reserva directa = reservaPendiente(apto, noviembre(10, 12));

            // Act
            ExcepcionNegocio error = capturar(() -> gestor.crear(solicitudExterna(noviembre(10, 11), "VE-1"),
                    ocupacion(apto, List.of(directa)), contexto(), AHORA));

            // Assert
            assertThat(error.getCodigo()).isEqualTo(CodigoError.NOCHES_NO_DISPONIBLES);
        }

        @Test
        @DisplayName("Cumplida: la estancia empieza el día de salida de la anterior")
        void estanciaContiguaSePermite() {
            // Arrange
            Apartamento apto = apartamento("BH-201");
            Reserva existente = reservaPendiente(apto, noviembre(10, 12));

            // Act
            Reserva nueva = gestor.crear(solicitud(noviembre(12, 14)), ocupacion(apto, List.of(existente)),
                    contexto(), AHORA);

            // Assert
            assertThat(nueva.esActiva()).isTrue();
        }

        @Test
        @DisplayName("Cumplida: una reserva cancelada no retiene las noches")
        void reservaCanceladaNoBloquea() {
            // Arrange
            Apartamento apto = apartamento("BH-201");
            Reserva cancelada = reservaPendiente(apto, noviembre(10, 12));
            cancelada.cancelar(AHORA);

            // Act
            Reserva nueva = gestor.crear(solicitud(noviembre(10, 12)), ocupacion(apto, List.of(cancelada)),
                    contexto(), AHORA);

            // Assert
            assertThat(nueva.esActiva()).isTrue();
        }
    }

    @Nested
    @DisplayName("RN-02: capacidad")
    class Capacidad {

        @Test
        @DisplayName("Violada: los niños no facturables también cuentan para la capacidad")
        void ninosCuentanParaCapacidad() {
            // Arrange
            Ocupante bebe1 = ocupanteNacidoEl(LocalDate.of(2025, 1, 1));
            Ocupante bebe2 = ocupanteNacidoEl(LocalDate.of(2025, 2, 1));
            SolicitudReserva cincoOcupantes = solicitud(noviembre(10, 12), adulto("A"), adulto("B"), bebe1, bebe2);

            // Act
            ExcepcionNegocio error = capturar(() -> gestor.crear(cincoOcupantes, libre(apartamento("BH-201")),
                    contexto(), AHORA));

            // Assert
            assertThat(error.getCodigo()).isEqualTo(CodigoError.CAPACIDAD_EXCEDIDA);
        }

        @Test
        @DisplayName("Cumplida: grupo igual a la capacidad")
        void grupoIgualALaCapacidad() {
            // Arrange
            SolicitudReserva cuatroOcupantes = solicitud(noviembre(10, 12), adulto("A"), adulto("B"), adulto("C"));

            // Act
            Reserva reserva = gestor.crear(cuatroOcupantes, libre(apartamento("BH-201")), contexto(), AHORA);

            // Assert
            assertThat(reserva.ocupantes()).hasSize(4);
        }
    }

    @Nested
    @DisplayName("RN-04: fecha de entrada")
    class FechaEntrada {

        @Test
        @DisplayName("Violada: entrada ayer")
        void entradaEnElPasado() {
            // Arrange
            SolicitudReserva desdeAyer = solicitud(estancia(HOY.minusDays(1), HOY.plusDays(1)));

            // Act
            ExcepcionNegocio error = capturar(() -> gestor.crear(desdeAyer, libre(apartamento("BH-201")),
                    contexto(), AHORA));

            // Assert
            assertThat(error.getCodigo()).isEqualTo(CodigoError.FECHA_ENTRADA_PASADA);
        }

        @Test
        @DisplayName("Cumplida: entrada hoy")
        void entradaHoy() {
            // Arrange
            SolicitudReserva desdeHoy = solicitud(estancia(HOY, HOY.plusDays(2)));

            // Act
            Reserva reserva = gestor.crear(desdeHoy, libre(apartamento("BH-201")), contexto(), AHORA);

            // Assert
            assertThat(reserva.getEstancia().fechaEntrada()).isEqualTo(HOY);
        }
    }

    @Nested
    @DisplayName("RN-07: bloqueos")
    class Bloqueos {

        @Test
        @DisplayName("Violada: un bloqueo sobre una noche del rango impide vender")
        void bloqueoImpideVender() {
            // Arrange
            Apartamento apto = apartamento("BH-201");
            Bloqueo pintura = Bloqueo.registrar("BH-201",
                    new RangoFechas(LocalDate.of(2026, 11, 11), LocalDate.of(2026, 11, 12)), "Pintura", List.of());

            // Act
            ExcepcionNegocio error = capturar(() -> gestor.crear(solicitud(noviembre(10, 13)),
                    ocupacion(apto, List.of(), pintura), contexto(), AHORA));

            // Assert
            assertThat(error.getCodigo()).isEqualTo(CodigoError.APARTAMENTO_BLOQUEADO);
        }

        @Test
        @DisplayName("El verificador informa que un rango con noches bloqueadas no está disponible")
        void verificadorDetectaBloqueo() {
            // Arrange
            Apartamento apto = apartamento("BH-201");
            Bloqueo pintura = Bloqueo.registrar("BH-201",
                    new RangoFechas(LocalDate.of(2026, 11, 11), LocalDate.of(2026, 11, 12)), "Pintura", List.of());

            // Act
            boolean disponible = new VerificadorDisponibilidad().estaDisponible(
                    ocupacion(apto, List.of(), pintura), noviembre(10, 13), 2, contexto(), HOY);

            // Assert
            assertThat(disponible).isFalse();
        }

        @Test
        @DisplayName("Cumplida: un bloqueo fuera del rango no afecta")
        void bloqueoFueraDelRango() {
            // Arrange
            Apartamento apto = apartamento("BH-201");
            Bloqueo pintura = Bloqueo.registrar("BH-201",
                    new RangoFechas(LocalDate.of(2026, 11, 13), LocalDate.of(2026, 11, 15)), "Pintura", List.of());

            // Act
            boolean disponible = new VerificadorDisponibilidad().estaDisponible(
                    ocupacion(apto, List.of(), pintura), noviembre(10, 13), 2, contexto(), HOY);

            // Assert
            assertThat(disponible).isTrue();
        }
    }

    @Nested
    @DisplayName("RN-20: tiempo de preparación")
    class TiempoPreparacion {

        @Test
        @DisplayName("Violada: 5 h de preparación exceden la ventana 11:00–15:00")
        void preparacionExcedeVentana() {
            // Arrange
            Apartamento apto = apartamento("BH-201");
            Reserva anterior = reservaPendiente(apto, noviembre(8, 10));
            ContextoReserva cincoHorasDePreparacion = contexto(parametrosConPreparacion(5));

            // Act
            ExcepcionNegocio error = capturar(() -> gestor.crear(solicitud(noviembre(10, 12)),
                    ocupacion(apto, List.of(anterior)), cincoHorasDePreparacion, AHORA));

            // Assert
            assertThat(error.getCodigo()).isEqualTo(CodigoError.TIEMPO_PREPARACION_INSUFICIENTE);
        }

        @Test
        @DisplayName("Violada también si la nueva estancia sale el día que entra la siguiente")
        void preparacionAntesDeLaSiguiente() {
            // Arrange
            Apartamento apto = apartamento("BH-201");
            Reserva siguiente = reservaPendiente(apto, noviembre(12, 14));
            ContextoReserva cincoHorasDePreparacion = contexto(parametrosConPreparacion(5));

            // Act
            ExcepcionNegocio error = capturar(() -> gestor.crear(solicitud(noviembre(10, 12)),
                    ocupacion(apto, List.of(siguiente)), cincoHorasDePreparacion, AHORA));

            // Assert
            assertThat(error.getCodigo()).isEqualTo(CodigoError.TIEMPO_PREPARACION_INSUFICIENTE);
        }

        @Test
        @DisplayName("Cumplida: 3 h caben en la ventana de 4 h")
        void preparacionDentroDeVentana() {
            // Arrange
            Apartamento apto = apartamento("BH-201");
            Reserva anterior = reservaPendiente(apto, noviembre(8, 10));

            // Act
            Reserva nueva = gestor.crear(solicitud(noviembre(10, 12)), ocupacion(apto, List.of(anterior)),
                    contexto(), AHORA);

            // Assert
            assertThat(nueva.esActiva()).isTrue();
        }

        @Test
        @DisplayName("Cumplida: con un día libre entre estancias no importa la preparación")
        void diaLibreEntreEstancias() {
            // Arrange
            Apartamento apto = apartamento("BH-201");
            Reserva anterior = reservaPendiente(apto, noviembre(8, 10));

            // Act
            Reserva nueva = gestor.crear(solicitud(noviembre(11, 12)), ocupacion(apto, List.of(anterior)),
                    contexto(parametrosConPreparacion(5)), AHORA);

            // Assert
            assertThat(nueva.esActiva()).isTrue();
        }
    }

    @Test
    @DisplayName("Validación 3: un apartamento inactivo no se vende")
    void apartamentoInactivo() {
        // Arrange
        Apartamento apto = apartamento("BH-201");
        apto.desactivar();

        // Act
        ExcepcionNegocio error = capturar(() -> gestor.crear(solicitud(noviembre(10, 12)), libre(apto),
                contexto(), AHORA));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.APARTAMENTO_INACTIVO);
    }

    @Test
    @DisplayName("Validación 3: un apartamento sin tarifas completas no se vende")
    void apartamentoSinTarifasCompletas() {
        // Arrange
        TablaTarifas soloBase = new TablaTarifas(List.of(
                new Tarifa("BH-201", BASE, Dinero.de(85_000), LocalDateTime.of(2026, 1, 1, 0, 0))));
        ContextoReserva contexto = new ContextoReserva(parametros(), calendario(), soloBase, politicaV1());

        // Act
        ExcepcionNegocio error = capturar(() -> gestor.crear(solicitud(noviembre(10, 12)),
                libre(apartamento("BH-201")), contexto, AHORA));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.TARIFAS_INCOMPLETAS);
    }

    @Test
    @DisplayName("Las validaciones se aplican en orden: la fecha pasada se reporta antes que la capacidad")
    void ordenDeLasValidaciones() {
        // Arrange: la solicitud viola a la vez la fecha de entrada (RN-04) y la capacidad (RN-02)
        SolicitudReserva violaDosReglas = solicitud(estancia(HOY.minusDays(1), HOY.plusDays(1)),
                adulto("A"), adulto("B"), adulto("C"), adulto("D"), adulto("E"));

        // Act
        ExcepcionNegocio error = capturar(() -> gestor.crear(violaDosReglas, libre(apartamento("BH-201")),
                contexto(), AHORA));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.FECHA_ENTRADA_PASADA);
    }

    @Test
    @DisplayName("El titular debe ser un ocupante facturable")
    void titularNoFacturable() {
        // Arrange
        Titular menor = new Titular("Niño", "TI-1", null, null, LocalDate.of(2023, 1, 1));
        SolicitudReserva solicitud = new SolicitudReserva(menor, List.of(), noviembre(10, 12), LocalTime.NOON,
                Canal.PORTAL, null, 0);

        // Act
        ExcepcionNegocio error = capturar(() -> gestor.crear(solicitud, libre(apartamento("BH-201")),
                contexto(), AHORA));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.TITULAR_NO_FACTURABLE);
    }

    @Nested
    @DisplayName("RP-01: estancia mínima por temporada")
    class EstanciaMinima {

        @Test
        @DisplayName("Violada: una noche en FIN_DE_ANIO")
        void unaNocheEnFinDeAnio() {
            // Arrange
            SolicitudReserva unaNoche = solicitud(estancia(LocalDate.of(2026, 12, 24), LocalDate.of(2026, 12, 25)));

            // Act
            ExcepcionNegocio error = capturar(() -> gestor.crear(unaNoche, libre(apartamento("BH-201")),
                    contexto(), AHORA));

            // Assert
            assertThat(error.getCodigo()).isEqualTo(CodigoError.ESTANCIA_MINIMA_NO_CUMPLIDA);
        }

        @Test
        @DisplayName("Violada: una estancia corta que solo toca la última noche de la temporada")
        void tocaLaTemporadaAlFinal() {
            // Arrange
            SolicitudReserva ultimaNoche = solicitud(estancia(LocalDate.of(2027, 1, 15), LocalDate.of(2027, 1, 16)));

            // Act
            ExcepcionNegocio error = capturar(() -> gestor.crear(ultimaNoche, libre(apartamento("BH-201")),
                    contexto(), AHORA));

            // Assert
            assertThat(error.getCodigo()).isEqualTo(CodigoError.ESTANCIA_MINIMA_NO_CUMPLIDA);
        }

        @Test
        @DisplayName("Cumplida: dos noches en FIN_DE_ANIO")
        void dosNochesEnFinDeAnio() {
            // Arrange
            SolicitudReserva dosNoches = solicitud(estancia(LocalDate.of(2026, 12, 24), LocalDate.of(2026, 12, 26)));

            // Act
            Reserva reserva = gestor.crear(dosNoches, libre(apartamento("BH-201")), contexto(), AHORA);

            // Assert
            assertThat(reserva.getEstancia().noches()).isEqualTo(2);
        }

        @Test
        @DisplayName("Cumplida: una noche en temporada BASE")
        void unaNocheEnBase() {
            // Arrange
            SolicitudReserva unaNoche = solicitud(noviembre(10, 11));

            // Act
            Reserva reserva = gestor.crear(unaNoche, libre(apartamento("BH-201")), contexto(), AHORA);

            // Assert
            assertThat(reserva.getEstancia().noches()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("RP-03: mascotas")
    class Mascotas {

        private SolicitudReserva conMascotas(int mascotas) {
            return new SolicitudReserva(titular(), List.of(), noviembre(10, 12), LocalTime.NOON, Canal.DIRECTO,
                    null, mascotas);
        }

        @Test
        @DisplayName("Violada: apartamento que no admite mascotas")
        void apartamentoSinMascotas() {
            // Arrange
            SolicitudReserva unaMascota = conMascotas(1);

            // Act
            ExcepcionNegocio error = capturar(() -> gestor.crear(unaMascota, libre(apartamento("BH-201")),
                    contexto(), AHORA));

            // Assert
            assertThat(error.getCodigo()).isEqualTo(CodigoError.MASCOTAS_NO_PERMITIDAS);
        }

        @Test
        @DisplayName("Violada: más mascotas que el máximo")
        void excedeMaximo() {
            // Arrange
            SolicitudReserva dosMascotas = conMascotas(2);

            // Act
            ExcepcionNegocio error = capturar(() -> gestor.crear(dosMascotas, libre(apartamento("BH-202")),
                    contexto(), AHORA));

            // Assert
            assertThat(error.getCodigo()).isEqualTo(CodigoError.MASCOTAS_EXCEDIDAS);
        }

        @Test
        @DisplayName("Cumplida: una mascota en BH-202 genera su cargo en el folio")
        void mascotaPermitida() {
            // Arrange
            SolicitudReserva unaMascota = conMascotas(1);

            // Act
            Reserva reserva = gestor.crear(unaMascota, libre(apartamento("BH-202")), contexto(), AHORA);

            // Assert
            assertThat(reserva.getFolio().getCargos()).extracting(c -> c.tipo())
                    .containsExactly(TipoCargo.ALOJAMIENTO, TipoCargo.MASCOTA);
            assertThat(reserva.getFolio().saldo()).isEqualTo(Dinero.de(170_000 + 40_000));
        }
    }
}
