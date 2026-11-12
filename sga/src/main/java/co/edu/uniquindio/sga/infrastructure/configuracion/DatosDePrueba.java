package co.edu.uniquindio.sga.infrastructure.configuracion;

import co.edu.uniquindio.sga.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga.domain.alojamiento.AlojamientoRepositorio;
import co.edu.uniquindio.sga.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga.domain.alojamiento.ServicioAdicional;
import co.edu.uniquindio.sga.domain.apartamento.*;
import co.edu.uniquindio.sga.domain.canal.*;
import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.Dinero;
import co.edu.uniquindio.sga.domain.comun.RangoFechas;
import co.edu.uniquindio.sga.domain.comun.Reloj;
import co.edu.uniquindio.sga.domain.folio.MedioPago;
import co.edu.uniquindio.sga.domain.folio.TipoCargo;
import co.edu.uniquindio.sga.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.politica.PoliticaRepositorio;
import co.edu.uniquindio.sga.domain.politica.TramoCancelacion;
import co.edu.uniquindio.sga.domain.reserva.*;
import co.edu.uniquindio.sga.domain.tarifa.*;
import co.edu.uniquindio.sga.domain.usuario.CifradorContrasenas;
import co.edu.uniquindio.sga.domain.usuario.Rol;
import co.edu.uniquindio.sga.domain.usuario.Usuario;
import co.edu.uniquindio.sga.domain.usuario.UsuarioRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Carga al iniciar los datos de la Ficha del Alojamiento "Aldea Bahareque" y un conjunto
 * de reservas que cubre todos los estados y los tres canales. Las fechas se calculan
 * desde el día actual para que la demostración siempre tenga llegadas y salidas "de hoy".
 */
@Component
public class DatosDePrueba implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DatosDePrueba.class);
    private static final String CONTRASENA_POR_DEFECTO = "Bahareque2026";

    private static final MedioPago EFECTIVO = new MedioPago("EFECTIVO");
    private static final MedioPago TRANSFERENCIA = new MedioPago("TRANSFERENCIA");
    private static final MedioPago TARJETA = new MedioPago("TARJETA");

    /** Tarifas por ocupante facturable por noche: BASE, MITAD_DE_ANIO, SEMANA_SANTA, FIN_DE_ANIO. */
    private static final Map<String, long[]> TARIFAS = Map.of(
            "BH-101", new long[]{95_000, 120_000, 140_000, 140_000},
            "BH-102", new long[]{95_000, 120_000, 140_000, 140_000},
            "BH-201", new long[]{85_000, 105_000, 125_000, 125_000},
            "BH-202", new long[]{85_000, 105_000, 125_000, 125_000},
            "BH-203", new long[]{85_000, 105_000, 125_000, 125_000},
            "BH-301", new long[]{95_000, 120_000, 140_000, 140_000},
            "BH-302", new long[]{75_000, 95_000, 110_000, 110_000});

    private final AlojamientoRepositorio alojamientos;
    private final TemporadaRepositorio temporadas;
    private final TarifaRepositorio tarifas;
    private final PoliticaRepositorio politicas;
    private final ApartamentoRepositorio apartamentos;
    private final BloqueoRepositorio bloqueos;
    private final ReservaRepositorio reservas;
    private final UsuarioRepositorio usuarios;
    private final CifradorContrasenas cifrador;
    private final GestorReservas gestor;
    private final Reloj reloj;
    private final PropiedadesSga propiedades;
    private final TransactionTemplate transaccion;
    private final CanalVentaRepositorio canales;
    private final ConflictoCanalRepositorio conflictos;
    private final BitacoraCanalRepositorio bitacora;

    public DatosDePrueba(AlojamientoRepositorio alojamientos, TemporadaRepositorio temporadas,
                         TarifaRepositorio tarifas, PoliticaRepositorio politicas, ApartamentoRepositorio apartamentos,
                         BloqueoRepositorio bloqueos, ReservaRepositorio reservas, UsuarioRepositorio usuarios,
                         CifradorContrasenas cifrador, GestorReservas gestor, Reloj reloj, PropiedadesSga propiedades,
                         TransactionTemplate transaccion, CanalVentaRepositorio canales,
                         ConflictoCanalRepositorio conflictos, BitacoraCanalRepositorio bitacora) {
        this.alojamientos = alojamientos;
        this.temporadas = temporadas;
        this.tarifas = tarifas;
        this.politicas = politicas;
        this.apartamentos = apartamentos;
        this.bloqueos = bloqueos;
        this.reservas = reservas;
        this.usuarios = usuarios;
        this.cifrador = cifrador;
        this.gestor = gestor;
        this.reloj = reloj;
        this.propiedades = propiedades;
        this.transaccion = transaccion;
        this.canales = canales;
        this.conflictos = conflictos;
        this.bitacora = bitacora;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!propiedades.datosPrueba().habilitados() || alojamientos.obtener().isPresent()) {
            return;
        }
        transaccion.executeWithoutResult(estado -> cargar());
        LOG.info("Datos de prueba de Aldea Bahareque cargados.");
    }

    private void cargar() {
        LocalDate hoy = reloj.hoy();
        ParametrosAlojamiento parametros = cargarAlojamiento();
        CalendarioTemporadas calendario = cargarTemporadas(hoy.getYear());
        PoliticaCancelacion politica = new PoliticaCancelacion(1, hoy.minusDays(90).atStartOfDay(), List.of(
                new TramoCancelacion(15, BigDecimal.ZERO),
                new TramoCancelacion(7, BigDecimal.valueOf(30)),
                new TramoCancelacion(0, BigDecimal.valueOf(60))), BigDecimal.valueOf(30));
        politicas.guardar(politica);
        cargarTarifas(hoy.minusDays(60).atStartOfDay());
        cargarApartamentos(calendario);
        Map<String, Usuario> cuentas = cargarUsuarios();
        ContextoReserva contexto = new ContextoReserva(parametros, calendario, tarifas.tabla(), politica);
        cargarCanales();
        cargarReservas(hoy, contexto, cuentas);
        cargarBloqueoYFueraDeServicio(hoy);
        cargarEventosDeCanal(hoy);
    }

    // --- Alojamiento y configuración ---

    private ParametrosAlojamiento cargarAlojamiento() {
        ParametrosAlojamiento parametros = new ParametrosAlojamiento(5, LocalTime.of(15, 0), LocalTime.of(11, 0), 3, 24,
                LocalTime.of(22, 0), BigDecimal.valueOf(30), 1, Dinero.de(40_000), 7, BigDecimal.TEN, 2);
        alojamientos.guardar(Alojamiento.crear("ALDEA-BAHAREQUE", "Aldea Bahareque",
                "Siete apartamentos independientes en una edificación de estilo bahareque, pensados para parejas, "
                        + "familias y grupos que visitan el Paisaje Cultural Cafetero.",
                "Filandia, Quindío", "Calle 6 # 4-32, a tres cuadras del parque principal",
                new BigDecimal("4.674600"), new BigDecimal("-75.658200"),
                List.of("Silencio de 22:00 a 7:00", "Prohibido fumar dentro de los apartamentos",
                        "No se permiten fiestas ni eventos",
                        "Los visitantes no registrados no pueden pernoctar"),
                List.of(new ServicioAdicional("DESAYUNO", "Desayuno típico", "Desayuno servido en el apartamento",
                                true, Dinero.de(25_000), "por persona por día"),
                        new ServicioAdicional("TOUR_CAFE", "Tour de café", "Recorrido por una finca cafetera",
                                true, Dinero.de(80_000), "por persona"),
                        new ServicioAdicional("TRASLADO", "Traslado desde la terminal de Armenia", "",
                                true, Dinero.de(90_000), "por trayecto"),
                        new ServicioAdicional("PARQUEADERO", "Parqueadero", "Sujeto a disponibilidad",
                                false, Dinero.CERO, "por estancia")),
                Set.of(EFECTIVO, TRANSFERENCIA, TARJETA), parametros));
        return parametros;
    }

    private CalendarioTemporadas cargarTemporadas(int anio) {
        List<RangoFechas> finDeAnio = new ArrayList<>();
        List<RangoFechas> mitadDeAnio = new ArrayList<>();
        List<RangoFechas> semanaSanta = new ArrayList<>();
        for (int a = anio - 1; a <= anio + 1; a++) {
            finDeAnio.add(new RangoFechas(LocalDate.of(a, 12, 15), LocalDate.of(a + 1, 1, 16)));
            mitadDeAnio.add(new RangoFechas(LocalDate.of(a, 6, 15), LocalDate.of(a, 7, 16)));
            LocalDate pascua = domingoDePascua(a);
            semanaSanta.add(new RangoFechas(pascua.minusDays(7), pascua.plusDays(1)));
        }
        CalendarioTemporadas calendario = new CalendarioTemporadas(List.of(
                Temporada.base("BASE", 1),
                new Temporada("MITAD_DE_ANIO", false, mitadDeAnio, 1),
                new Temporada("SEMANA_SANTA", false, semanaSanta, 2),
                new Temporada("FIN_DE_ANIO", false, finDeAnio, 2)));
        temporadas.guardar(calendario);
        return calendario;
    }

    /** Fecha del domingo de Pascua (algoritmo de Butcher para el calendario gregoriano). */
    static LocalDate domingoDePascua(int anio) {
        int a = anio % 19, b = anio / 100, c = anio % 100, d = b / 4, e = b % 4, f = (b + 8) / 25;
        int g = (b - f + 1) / 3, h = (19 * a + b - d - g + 15) % 30, i = c / 4, k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7, m = (a + 11 * h + 22 * l) / 451;
        int mes = (h + l - 7 * m + 114) / 31, dia = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(anio, mes, dia);
    }

    private void cargarTarifas(LocalDateTime vigencia) {
        String[] nombres = {"BASE", "MITAD_DE_ANIO", "SEMANA_SANTA", "FIN_DE_ANIO"};
        TARIFAS.forEach((codigo, valores) -> {
            for (int i = 0; i < nombres.length; i++) {
                tarifas.registrar(new Tarifa(codigo, nombres[i], Dinero.de(valores[i]), vigencia));
            }
        });
    }

    private void cargarApartamentos(CalendarioTemporadas calendario) {
        TablaTarifas tabla = tarifas.tabla();
        List<Apartamento> inventario = List.of(
                Apartamento.crear("BH-101", "Guadua", "Apartamento para pareja con balcón y vista al valle.", 1, 2,
                        Set.of("balcon", "vista-valle", "cama-queen", "admite-mascotas"), true),
                Apartamento.crear("BH-102", "Yarumo", "Primer piso sin escaleras, ideal para personas con movilidad reducida.",
                        1, 3, Set.of("accesible", "sofa-cama"), false),
                Apartamento.crear("BH-201", "Cafetal", "Dos dormitorios con cocina completa y lavadora.", 2, 4,
                        Set.of("cocina-completa", "lavadora"), false),
                Apartamento.crear("BH-202", "Barranquero", "Dos dormitorios con patio privado. Admite mascotas.", 2, 4,
                        Set.of("patio", "admite-mascotas"), true),
                Apartamento.crear("BH-203", "Palma de Cera", "Dos dormitorios con vista panorámica.", 2, 5,
                        Set.of("vista-panoramica", "sofa-cama"), false),
                Apartamento.crear("BH-301", "Mirador", "Tres dormitorios con terraza privada y jacuzzi.", 3, 6,
                        Set.of("terraza", "jacuzzi", "vista-panoramica"), false),
                Apartamento.crear("BH-302", "Quimbaya", "Apartamento familiar con dos baños y comedor para ocho.", 3, 8,
                        Set.of("familiar", "dos-banos"), false));
        for (Apartamento apartamento : inventario) {
            String semilla = apartamento.getCodigo().toLowerCase();
            apartamento.definirImagenes(List.of(
                    new ImagenApartamento("/imagenes/" + semilla + "-1.svg", true),
                    new ImagenApartamento("/imagenes/" + semilla + "-2.svg", false),
                    new ImagenApartamento("/imagenes/" + semilla + "-3.svg", false)));
            apartamento.activar(tabla, calendario);
            apartamentos.guardar(apartamento);
        }
    }

    private Map<String, Usuario> cargarUsuarios() {
        String configurada = propiedades.datosPrueba().contrasena();
        String clave = configurada == null || configurada.isBlank() ? CONTRASENA_POR_DEFECTO : configurada;
        Usuario admin = Usuario.crear("Valentina Arango", "admin@aldeabahareque.co", clave, "3104567890", "1094111222",
                LocalDate.of(1985, 4, 12), Rol.ADMINISTRADOR, cifrador);
        Usuario recepcion = Usuario.crear("Santiago Ríos", "recepcion@aldeabahareque.co", clave, "3115678901",
                "1094222333", LocalDate.of(1993, 9, 3), Rol.RECEPCIONISTA, cifrador);
        Usuario servicio = Usuario.crear("Luz Marina Cardona", "servicio@aldeabahareque.co", clave, "3126789012",
                "1094333444", LocalDate.of(1979, 1, 25), Rol.PERSONAL_SERVICIO, cifrador);
        Usuario laura = Usuario.crear("Laura Gómez", "laura.gomez@correo.com", clave, "3001234567", "1094000111",
                LocalDate.of(1990, 5, 10), Rol.HUESPED, cifrador);
        Usuario carlos = Usuario.crear("Carlos Mejía", "carlos.mejia@correo.com", clave, "3012345678", "1094555666",
                LocalDate.of(1982, 11, 30), Rol.HUESPED, cifrador);
        List.of(admin, recepcion, servicio, laura, carlos).forEach(usuarios::guardar);
        return Map.of("laura", laura, "carlos", carlos);
    }

    // --- Canales externos ---

    /**
     * ViajaEje es el canal del simulador (colección Bruno). Su credencial se toma de
     * SGA_CREDENCIAL_CANAL_DEMO; si no está configurada se genera una y se muestra en el log.
     */
    private void cargarCanales() {
        String configurada = propiedades.datosPrueba().credencialCanal();
        if (configurada != null && !configurada.isBlank()) {
            canales.guardar(CanalVenta.registrarConCredencial("ViajaEje", "ViajaEje - agencia en línea",
                    TipoCanal.AGENCIA_EN_LINEA, configurada, cifrador));
        } else {
            CanalVenta.Credencial generada = CanalVenta.registrar("ViajaEje", "ViajaEje - agencia en línea",
                    TipoCanal.AGENCIA_EN_LINEA, cifrador);
            canales.guardar(generada.canal());
            LOG.warn("Credencial generada para el canal de demostración ViajaEje: {} "
                    + "(configure SGA_CREDENCIAL_CANAL_DEMO para fijarla)", generada.credencialEnClaro());
        }
        CanalVenta rutaCafetera = CanalVenta.registrar("RutaCafetera", "Ruta Cafetera Tours", TipoCanal.AGENCIA_DE_VIAJES,
                cifrador).canal();
        rutaCafetera.desactivar();
        canales.guardar(rutaCafetera);
    }

    /** Bitácora de la reserva externa de ViajaEje y un conflicto pendiente de revisión. */
    private void cargarEventosDeCanal(LocalDate hoy) {
        Reserva externa = reservas.buscarPorReferenciaExterna(new ReferenciaExterna("ViajaEje", "VE-1001")).orElseThrow();
        bitacora.registrar(EventoCanal.de("ViajaEje", TipoEventoCanal.CONSULTA_DISPONIBILIDAD, null, null,
                "Del " + hoy.plusDays(5) + " al " + hoy.plusDays(8) + ": 5 apartamento(s) disponible(s)", hace(hoy, 3)));
        bitacora.registrar(EventoCanal.de("ViajaEje", TipoEventoCanal.RESERVA_ACEPTADA, "VE-1001", externa.getId(),
                "Reserva creada en BH-101 del " + hoy.plusDays(5) + " al " + hoy.plusDays(8), hace(hoy, 3).plusMinutes(2)));
        Estancia chocada = new Estancia(hoy.plusDays(6), hoy.plusDays(9));
        conflictos.guardar(new ConflictoCanal(UUID.randomUUID().toString(), new ReferenciaExterna("ViajaEje", "VE-1002"),
                "BH-101", chocada, "Oliver Smith", CodigoError.NOCHES_NO_DISPONIBLES,
                hace(hoy, 2), null));
        bitacora.registrar(EventoCanal.de("ViajaEje", TipoEventoCanal.RESERVA_EN_CONFLICTO, "VE-1002", null,
                "Rechazada y registrada como conflicto: "
                        + CodigoError.NOCHES_NO_DISPONIBLES.mensaje(), hace(hoy, 2)));
    }

    // --- Reservas en todos los estados y por los tres canales ---

    private void cargarReservas(LocalDate hoy, ContextoReserva contexto, Map<String, Usuario> cuentas) {
        ParametrosAlojamiento p = contexto.parametros();
        Titular laura = titular(cuentas.get("laura"));
        Titular carlos = titular(cuentas.get("carlos"));

        // FINALIZADA (portal): estancia pasada, pagada y cerrada; el apartamento ya se preparó.
        Apartamento yarumo = apartamento("BH-102");
        Reserva finalizada = crear(solicitud(carlos, hoy.minusDays(15), hoy.minusDays(12), Canal.PORTAL, null, 0,
                new Ocupante("Diana Mejía", LocalDate.of(1984, 2, 14))), yarumo, contexto, hace(hoy, 20));
        pagarAnticipo(finalizada, p, hace(hoy, 19), TRANSFERENCIA);
        finalizada.confirmar(p);
        finalizada.registrarLlegada(hoy.minusDays(15), yarumo);
        finalizada.getFolio().agregarCargo(TipoCargo.SERVICIO_ADICIONAL, Dinero.de(100_000), "Desayuno típico x4",
                hace(hoy, 14));
        finalizada.getFolio().registrarPago(finalizada.getFolio().saldo(), TARJETA, hace(hoy, 12));
        finalizada.registrarSalida(yarumo, null);
        yarumo.iniciarPreparacion();
        yarumo.terminarPreparacion();
        guardar(finalizada, yarumo);

        // EN_CURSO (directo): el grupo sale hoy y el folio aún tiene saldo.
        Apartamento cafetal = apartamento("BH-201");
        Reserva enCurso = crear(solicitud(new Titular("Andrés Restrepo", "71222333", "andres.restrepo@correo.com",
                        "3137894561", LocalDate.of(1978, 7, 8)), hoy.minusDays(2), hoy, Canal.DIRECTO, null, 0,
                new Ocupante("Paula Restrepo", LocalDate.of(1980, 3, 19)),
                new Ocupante("Tomás Restrepo", hoy.minusYears(3))), cafetal, contexto, hace(hoy, 7));
        pagarAnticipo(enCurso, p, hace(hoy, 6), EFECTIVO);
        enCurso.confirmar(p);
        enCurso.registrarLlegada(hoy.minusDays(2), cafetal);
        enCurso.getFolio().agregarCargo(TipoCargo.SERVICIO_ADICIONAL, Dinero.de(50_000), "Desayuno típico x2",
                hace(hoy, 1));
        guardar(enCurso, cafetal);

        // CONFIRMADA (externo): llega en cinco días desde el canal ViajaEje.
        Apartamento guadua = apartamento("BH-101");
        Reserva externa = crear(new SolicitudReserva(new Titular("Emily Carter", "P-55667788", "emily.carter@mail.com",
                        "+1 555 0100", LocalDate.of(1991, 6, 2)),
                List.of(new Ocupante("James Carter", LocalDate.of(1990, 1, 17))),
                new Estancia(hoy.plusDays(5), hoy.plusDays(8)), LocalTime.of(16, 0), Canal.EXTERNO,
                new ReferenciaExterna("ViajaEje", "VE-1001"), 0), guadua, contexto, hace(hoy, 3));
        pagarAnticipo(externa, p, hace(hoy, 3), TARJETA);
        externa.confirmar(p);
        guardar(externa, guadua);

        // CONFIRMADA (directo): llegada prevista para hoy.
        Apartamento palma = apartamento("BH-203");
        Reserva llegaHoy = crear(solicitud(new Titular("María Fernanda Ospina", "43555666", "mafe.ospina@correo.com",
                        "3149876543", LocalDate.of(1988, 12, 1)), hoy, hoy.plusDays(3), Canal.DIRECTO,
                LocalTime.of(17, 30), 0,
                new Ocupante("Camilo Ospina", LocalDate.of(1987, 5, 23)),
                new Ocupante("Sara Ospina", hoy.minusYears(8)),
                new Ocupante("Martín Ospina", hoy.minusYears(4))), palma, contexto, hace(hoy, 10));
        pagarAnticipo(llegaHoy, p, hace(hoy, 9), TRANSFERENCIA);
        llegaHoy.confirmar(p);
        guardar(llegaHoy, palma);

        // PENDIENTE (portal): recién creada, con mascota, a la espera del anticipo.
        Apartamento barranquero = apartamento("BH-202");
        Reserva pendiente = crear(solicitud(laura, hoy.plusDays(10), hoy.plusDays(13), Canal.PORTAL,
                LocalTime.of(15, 30), 1, new Ocupante("Felipe Gómez", LocalDate.of(1989, 8, 4))),
                barranquero, contexto, reloj.ahora());
        guardar(pendiente, barranquero);

        // CANCELADA (portal): cancelada con más de 15 días de antelación; el anticipo queda a favor.
        Apartamento mirador = apartamento("BH-301");
        Reserva cancelada = crear(solicitud(laura, hoy.plusDays(20), hoy.plusDays(23), Canal.PORTAL, null, 0,
                new Ocupante("Felipe Gómez", LocalDate.of(1989, 8, 4)),
                new Ocupante("Ana Gómez", LocalDate.of(1962, 4, 30))), mirador, contexto, hace(hoy, 12));
        pagarAnticipo(cancelada, p, hace(hoy, 11), TRANSFERENCIA);
        cancelada.confirmar(p);
        cancelada.cancelar(hace(hoy, 5));
        guardar(cancelada, mirador);

        // NO_SHOW (directo): el titular no llegó antes de las 22:00 del día de entrada.
        Reserva noShow = crear(solicitud(new Titular("Julián Castaño", "10222333", "julian.castano@correo.com",
                        "3161239876", LocalDate.of(1995, 10, 15)), hoy.minusDays(3), hoy.minusDays(1), Canal.DIRECTO,
                LocalTime.of(20, 0), 0, new Ocupante("Natalia Duque", LocalDate.of(1996, 2, 2))),
                mirador, contexto, hace(hoy, 9));
        pagarAnticipo(noShow, p, hace(hoy, 8), EFECTIVO);
        noShow.confirmar(p);
        noShow.declararNoShow(hoy.minusDays(3).atTime(22, 30), p);
        guardar(noShow, mirador);
    }

    private void cargarBloqueoYFueraDeServicio(LocalDate hoy) {
        Apartamento quimbaya = apartamento("BH-302");
        quimbaya.declararFueraDeServicio();
        apartamentos.guardar(quimbaya);
        bloqueos.guardar(Bloqueo.registrar(quimbaya.getCodigo(), new RangoFechas(hoy, hoy.plusDays(7)),
                "Reparación de la red hidráulica del segundo baño", List.of()));
    }

    // --- Apoyo ---

    private Reserva crear(SolicitudReserva solicitud, Apartamento apartamento, ContextoReserva contexto,
                          LocalDateTime cuando) {
        OcupacionApartamento ocupacion = new OcupacionApartamento(apartamento,
                reservas.activasDe(apartamento.getCodigo()), bloqueos.vigentesDe(apartamento.getCodigo()));
        return gestor.crear(solicitud, ocupacion, contexto, cuando);
    }

    private void guardar(Reserva reserva, Apartamento apartamento) {
        reservas.guardar(reserva);
        apartamentos.guardar(apartamento);
    }

    private Apartamento apartamento(String codigo) {
        return apartamentos.buscar(codigo).orElseThrow();
    }

    private static void pagarAnticipo(Reserva reserva, ParametrosAlojamiento p, LocalDateTime cuando, MedioPago medio) {
        reserva.getFolio().registrarPago(reserva.anticipoRequerido(p), medio, cuando);
    }

    private static SolicitudReserva solicitud(Titular titular, LocalDate entrada, LocalDate salida, Canal canal,
                                              LocalTime hora, int mascotas, Ocupante... acompanantes) {
        return new SolicitudReserva(titular, List.of(acompanantes), new Estancia(entrada, salida),
                hora == null ? LocalTime.of(15, 0) : hora, canal, null, mascotas);
    }

    private static Titular titular(Usuario u) {
        return new Titular(u.getNombre(), u.getDocumento(), u.getCorreo(), u.getTelefono(), u.getFechaNacimiento(),
                u.getId());
    }

    private static LocalDateTime hace(LocalDate hoy, int dias) {
        return hoy.minusDays(dias).atTime(10, 0);
    }
}
