package co.edu.uniquindio.sga.domain;

import co.edu.uniquindio.sga.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga.domain.apartamento.Bloqueo;
import co.edu.uniquindio.sga.domain.apartamento.ImagenApartamento;
import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.Dinero;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import co.edu.uniquindio.sga.domain.comun.RangoFechas;
import co.edu.uniquindio.sga.domain.folio.MedioPago;
import co.edu.uniquindio.sga.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.politica.TramoCancelacion;
import co.edu.uniquindio.sga.domain.reserva.*;
import co.edu.uniquindio.sga.domain.tarifa.CalendarioTemporadas;
import co.edu.uniquindio.sga.domain.tarifa.TablaTarifas;
import co.edu.uniquindio.sga.domain.tarifa.Tarifa;
import co.edu.uniquindio.sga.domain.tarifa.Temporada;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Datos de prueba con la Ficha del Alojamiento "Aldea Bahareque".
 * El "ahora" de las pruebas es fijo: 1 de octubre de 2026, 10:00.
 */
public final class Escenario {

    public static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 1, 10, 0);
    public static final LocalDate HOY = AHORA.toLocalDate();

    public static final String BASE = "BASE";
    public static final String FIN_DE_ANIO = "FIN_DE_ANIO";
    public static final String SEMANA_SANTA = "SEMANA_SANTA";
    public static final String MITAD_DE_ANIO = "MITAD_DE_ANIO";

    public static final MedioPago EFECTIVO = new MedioPago("EFECTIVO");
    public static final MedioPago TRANSFERENCIA = new MedioPago("TRANSFERENCIA");

    /** Tarifas por ocupante facturable por noche: BASE, MITAD_DE_ANIO, SEMANA_SANTA, FIN_DE_ANIO. */
    private static final Map<String, long[]> TARIFAS = Map.of(
            "BH-101", new long[]{95_000, 120_000, 140_000, 140_000},
            "BH-102", new long[]{95_000, 120_000, 140_000, 140_000},
            "BH-201", new long[]{85_000, 105_000, 125_000, 125_000},
            "BH-202", new long[]{85_000, 105_000, 125_000, 125_000},
            "BH-203", new long[]{85_000, 105_000, 125_000, 125_000},
            "BH-301", new long[]{95_000, 120_000, 140_000, 140_000},
            "BH-302", new long[]{75_000, 95_000, 110_000, 110_000});

    private Escenario() {
    }

    // --- Configuración ---

    public static ParametrosAlojamiento parametros() {
        return parametrosConPreparacion(3);
    }

    public static ParametrosAlojamiento parametrosConPreparacion(int horasPreparacion) {
        return new ParametrosAlojamiento(5, LocalTime.of(15, 0), LocalTime.of(11, 0), horasPreparacion, 24,
                LocalTime.of(22, 0), BigDecimal.valueOf(30), 1, Dinero.de(40_000), 7, BigDecimal.TEN, 2);
    }

    public static CalendarioTemporadas calendario() {
        return new CalendarioTemporadas(List.of(
                Temporada.base(BASE, 1),
                Temporada.de(FIN_DE_ANIO, 2, new RangoFechas(LocalDate.of(2026, 12, 15), LocalDate.of(2027, 1, 16))),
                Temporada.de(SEMANA_SANTA, 2, new RangoFechas(LocalDate.of(2027, 3, 21), LocalDate.of(2027, 3, 29))),
                Temporada.de(MITAD_DE_ANIO, 1, new RangoFechas(LocalDate.of(2027, 6, 15), LocalDate.of(2027, 7, 16)))));
    }

    public static TablaTarifas tarifas() {
        List<Tarifa> lista = new ArrayList<>();
        LocalDateTime vigencia = LocalDateTime.of(2026, 1, 1, 0, 0);
        TARIFAS.forEach((codigo, valores) -> {
            lista.add(new Tarifa(codigo, BASE, Dinero.de(valores[0]), vigencia));
            lista.add(new Tarifa(codigo, MITAD_DE_ANIO, Dinero.de(valores[1]), vigencia));
            lista.add(new Tarifa(codigo, SEMANA_SANTA, Dinero.de(valores[2]), vigencia));
            lista.add(new Tarifa(codigo, FIN_DE_ANIO, Dinero.de(valores[3]), vigencia));
        });
        return new TablaTarifas(lista);
    }

    public static PoliticaCancelacion politicaV1() {
        return new PoliticaCancelacion(1, LocalDateTime.of(2026, 1, 1, 0, 0), List.of(
                new TramoCancelacion(15, BigDecimal.ZERO),
                new TramoCancelacion(7, BigDecimal.valueOf(30)),
                new TramoCancelacion(0, BigDecimal.valueOf(60))),
                BigDecimal.valueOf(30));
    }

    public static ContextoReserva contexto() {
        return new ContextoReserva(parametros(), calendario(), tarifas(), politicaV1());
    }

    public static ContextoReserva contexto(ParametrosAlojamiento parametros) {
        return new ContextoReserva(parametros, calendario(), tarifas(), politicaV1());
    }

    // --- Inventario ---

    public static Apartamento apartamento(String codigo) {
        Apartamento apartamento = switch (codigo) {
            case "BH-101" -> Apartamento.crear(codigo, "Guadua", "Balcón con vista al valle", 1, 2, Set.of("balcon"), true);
            case "BH-102" -> Apartamento.crear(codigo, "Yarumo", "Primer piso accesible", 1, 3, Set.of("accesible"), false);
            case "BH-201" -> Apartamento.crear(codigo, "Cafetal", "Cocina completa y lavadora", 2, 4, Set.of("lavadora"), false);
            case "BH-202" -> Apartamento.crear(codigo, "Barranquero", "Patio privado", 2, 4, Set.of("patio"), true);
            case "BH-203" -> Apartamento.crear(codigo, "Palma de Cera", "Vista panorámica", 2, 5, Set.of("vista"), false);
            case "BH-301" -> Apartamento.crear(codigo, "Mirador", "Terraza con jacuzzi", 3, 6, Set.of("jacuzzi"), false);
            case "BH-302" -> Apartamento.crear(codigo, "Quimbaya", "Familiar, dos baños", 3, 8, Set.of("familiar"), false);
            default -> throw new IllegalArgumentException(codigo);
        };
        apartamento.definirImagenes(List.of(new ImagenApartamento("https://imagenes.sga/" + codigo + ".jpg", true)));
        apartamento.activar(tarifas(), calendario());
        return apartamento;
    }

    public static OcupacionApartamento ocupacion(Apartamento apartamento, List<Reserva> reservas, Bloqueo... bloqueos) {
        return new OcupacionApartamento(apartamento, reservas, List.of(bloqueos));
    }

    public static OcupacionApartamento libre(Apartamento apartamento) {
        return ocupacion(apartamento, List.of());
    }

    // --- Personas ---

    public static Titular titular() {
        return new Titular("Laura Gómez", "1094000111", "laura@correo.com", "3001234567", LocalDate.of(1990, 5, 10));
    }

    public static Ocupante adulto(String nombre) {
        return new Ocupante(nombre, LocalDate.of(1988, 3, 3));
    }

    public static Ocupante ocupanteNacidoEl(LocalDate fechaNacimiento) {
        return new Ocupante("Menor", fechaNacimiento);
    }

    // --- Reservas ---

    public static Estancia estancia(LocalDate entrada, LocalDate salida) {
        return new Estancia(entrada, salida);
    }

    public static Estancia noviembre(int diaEntrada, int diaSalida) {
        return new Estancia(LocalDate.of(2026, 11, diaEntrada), LocalDate.of(2026, 11, diaSalida));
    }

    public static SolicitudReserva solicitud(Estancia estancia, Ocupante... acompanantes) {
        return new SolicitudReserva(titular(), List.of(acompanantes), estancia, LocalTime.of(16, 0),
                Canal.DIRECTO, null, 0);
    }

    public static SolicitudReserva solicitudExterna(Estancia estancia, String idExterno, Ocupante... acompanantes) {
        return new SolicitudReserva(titular(), List.of(acompanantes), estancia, LocalTime.of(16, 0),
                Canal.EXTERNO, new ReferenciaExterna("ViajaEje", idExterno), 0);
    }

    /** Crea una reserva PENDIENTE sobre un apartamento libre. */
    public static Reserva reservaPendiente(Apartamento apartamento, Estancia estancia, Ocupante... acompanantes) {
        return new GestorReservas().crear(solicitud(estancia, acompanantes), libre(apartamento), contexto(), AHORA);
    }

    /** Paga el anticipo exigido y confirma. */
    public static Reserva confirmar(Reserva reserva) {
        reserva.getFolio().registrarPago(reserva.anticipoRequerido(parametros()), TRANSFERENCIA, AHORA);
        reserva.confirmar(parametros());
        return reserva;
    }

    // --- Aserciones ---

    /**
     * Ejecuta la acción (paso Act) y devuelve la excepción de negocio que lanzó, para
     * verificar su código en el paso Assert. Falla si la acción no viola ninguna regla.
     */
    public static ExcepcionNegocio capturar(ThrowingCallable accion) {
        Throwable error = catchThrowable(accion);
        assertThat(error).as("la acción debía violar una regla de negocio").isInstanceOf(ExcepcionNegocio.class);
        return (ExcepcionNegocio) error;
    }

    /** Verifica que la acción viole exactamente la regla indicada. */
    public static void violaRegla(CodigoError codigo, ThrowingCallable accion) {
        assertThatThrownBy(accion)
                .isInstanceOf(ExcepcionNegocio.class)
                .satisfies(e -> assertThat(((ExcepcionNegocio) e).getCodigo()).isEqualTo(codigo));
    }
}
