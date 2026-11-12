package co.edu.uniquindio.sga.infrastructure.rest;

import co.edu.uniquindio.sga.infrastructure.notificacion.NotificadorEnMemoria;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la API completa: seguridad, persistencia y datos de prueba reales.
 * Cada prueba usa fechas o apartamentos propios para no depender del orden de ejecución.
 */
@SpringBootTest(properties = "sga.reservas.intervalo-vencimiento-ms=3600000")
@AutoConfigureMockMvc
class ApiIntegracionTest {

    private static final String CLAVE = "Bahareque2026";
    private static final String ADMIN = "admin@aldeabahareque.co";
    private static final String RECEPCION = "recepcion@aldeabahareque.co";
    private static final String SERVICIO = "servicio@aldeabahareque.co";
    private static final String LAURA = "laura.gomez@correo.com";
    private static final String CARLOS = "carlos.mejia@correo.com";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private NotificadorEnMemoria notificador;

    private static LocalDate hoy() {
        return LocalDate.now(ZoneId.of("America/Bogota"));
    }

    // --- Apoyo ---

    private String token(String correo) throws Exception {
        return token(correo, CLAVE);
    }

    private String token(String correo, String clave) throws Exception {
        String cuerpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, clave)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.token");
    }

    private ResultActions ejecutar(MockHttpServletRequestBuilder solicitud, String token, String json) throws Exception {
        if (token != null) solicitud.header("Authorization", "Bearer " + token);
        if (json != null) solicitud.contentType(MediaType.APPLICATION_JSON).content(json);
        return mvc.perform(solicitud);
    }

    private static String leer(ResultActions resultado, String ruta) throws Exception {
        return JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), ruta).toString();
    }

    private String reservarPorPortal(String token, String apartamento, LocalDate entrada, LocalDate salida)
            throws Exception {
        ResultActions r = ejecutar(post("/api/reservas/portal"), token, """
                {"codigoApartamento":"%s","entrada":"%s","salida":"%s",
                 "acompanantes":[{"nombre":"Acompañante","fechaNacimiento":"1989-08-04"}],
                 "horaEstimadaLlegada":"16:00"}""".formatted(apartamento, entrada, salida))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.canal").value("PORTAL"));
        return leer(r, "$.id");
    }

    // --- Consultas públicas ---

    @Nested
    @DisplayName("Consultas públicas")
    class Publicas {

        @Test
        @DisplayName("El alojamiento y sus apartamentos a la venta se consultan sin sesión, paginados de a 10")
        void alojamientoYApartamentos() throws Exception {
            ejecutar(get("/api/alojamiento"), null, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombre").value("Aldea Bahareque"))
                    .andExpect(jsonPath("$.parametros.umbralEdadFacturable").value(5));
            ejecutar(get("/api/apartamentos"), null, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tamano").value(10))
                    .andExpect(jsonPath("$.contenido[*].codigo", hasItems("BH-101", "BH-302")));
        }

        @Test
        @DisplayName("La búsqueda devuelve tarjetas con valor estimado y excluye los bloqueados")
        void busqueda() throws Exception {
            LocalDate entrada = hoy().plusDays(1);
            ejecutar(post("/api/disponibilidad/busqueda"), null, """
                    {"entrada":"%s","salida":"%s","personas":2}""".formatted(entrada, entrada.plusDays(2)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contenido[0].valorEstimado").isNumber())
                    .andExpect(jsonPath("$.contenido[*].codigo", not(hasItem("BH-302"))));
        }

        @Test
        @DisplayName("La cotización desglosa noche por noche y no cobra a los menores del umbral")
        void cotizacion() throws Exception {
            LocalDate entrada = hoy().plusDays(120);
            ejecutar(post("/api/cotizaciones"), null, """
                    {"codigoApartamento":"BH-201","entrada":"%s","salida":"%s",
                     "fechasNacimiento":["1990-01-01","1992-02-02","%s"]}"""
                    .formatted(entrada, entrada.plusDays(2), hoy().minusYears(2)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.noches").value(2))
                    .andExpect(jsonPath("$.cotizacion.detalle.length()").value(2))
                    .andExpect(jsonPath("$.cotizacion.detalle[0].ocupantesFacturables").value(2));
        }

        @Test
        @DisplayName("Una solicitud incompleta responde 400 con el detalle por campo")
        void solicitudInvalida() throws Exception {
            ejecutar(post("/api/cotizaciones"), null, "{\"codigoApartamento\":\"BH-201\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"))
                    .andExpect(jsonPath("$.detalles", not(empty())));
        }
    }

    // --- Seguridad ---

    @Nested
    @DisplayName("Seguridad")
    class Seguridad {

        @Test
        @DisplayName("Sin sesión no se accede a las reservas (401) y un huésped no ve el listado general (403)")
        void rolesYAutenticacion() throws Exception {
            ejecutar(get("/api/reservas"), null, null).andExpect(status().isUnauthorized());
            ejecutar(get("/api/reservas"), token(LAURA), null).andExpect(status().isForbidden());
            ejecutar(get("/api/reservas"), token(RECEPCION), null).andExpect(status().isOk());
        }

        @Test
        @DisplayName("Credenciales incorrectas responden 401 con código de negocio")
        void credencialesInvalidas() throws Exception {
            ejecutar(post("/api/auth/login"), null, "{\"correo\":\"%s\",\"contrasena\":\"Otra1234\"}".formatted(LAURA))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
        }

        @Test
        @DisplayName("Un huésped no accede a reservas ni folios de otro huésped (restricción por propietario)")
        void restriccionPorPropietario() throws Exception {
            LocalDate entrada = hoy().plusDays(150);
            String id = reservarPorPortal(token(LAURA), "BH-102", entrada, entrada.plusDays(2));
            String carlos = token(CARLOS);
            ejecutar(get("/api/reservas/" + id), carlos, null).andExpect(status().isForbidden());
            ejecutar(get("/api/reservas/" + id + "/folio"), carlos, null).andExpect(status().isForbidden());
            ejecutar(post("/api/reservas/" + id + "/cancelacion"), carlos, null).andExpect(status().isForbidden());
            ejecutar(get("/api/reservas/" + id), token(LAURA), null).andExpect(status().isOk());
        }

        @Test
        @DisplayName("Registro, perfil y recuperación de contraseña con código de un solo uso")
        void registroYRecuperacion() throws Exception {
            String correo = "nuevo.huesped@correo.com";
            ejecutar(post("/api/auth/registro"), null, """
                    {"nombre":"Nuevo Huésped","correo":"%s","contrasena":"Inicial123","telefono":"3000000000",
                     "documento":"99887766","fechaNacimiento":"1995-05-05"}""".formatted(correo))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.usuario.rol").value("HUESPED"));
            ejecutar(post("/api/auth/registro"), null, """
                    {"nombre":"Otro","correo":"%s","contrasena":"Inicial123","telefono":"1","documento":"1",
                     "fechaNacimiento":"1995-05-05"}""".formatted(correo))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("CORREO_YA_REGISTRADO"));

            ejecutar(post("/api/auth/recuperacion"), null, "{\"correo\":\"%s\"}".formatted(correo))
                    .andExpect(status().isOk());
            String mensaje = notificador.ultimaPara(correo).orElseThrow().mensaje();
            String codigo = mensaje.replaceAll(".*es (\\d{6}).*", "$1");
            String restablecer = """
                    {"correo":"%s","codigo":"%s","nuevaContrasena":"Nueva2026X"}""".formatted(correo, codigo);
            ejecutar(post("/api/auth/recuperacion/restablecimiento"), null, restablecer).andExpect(status().isOk());
            ejecutar(post("/api/auth/recuperacion/restablecimiento"), null, restablecer)
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.codigo").value("CODIGO_RECUPERACION_INVALIDO"));

            String sesion = token(correo, "Nueva2026X");
            ejecutar(get("/api/perfil"), sesion, null).andExpect(jsonPath("$.correo").value(correo));
        }
    }

    // --- Reservas ---

    @Nested
    @DisplayName("Reservas")
    class Reservas {

        @Test
        @DisplayName("Portal → anticipo → confirmación; la doble venta se rechaza; la cancelación deja saldo a favor")
        void cicloCompleto() throws Exception {
            LocalDate entrada = hoy().plusDays(60);
            String laura = token(LAURA);
            String recepcion = token(RECEPCION);
            String id = reservarPorPortal(laura, "BH-203", entrada, entrada.plusDays(3));

            ejecutar(post("/api/reservas/directas"), recepcion, """
                    {"titular":{"nombre":"Pedro Pérez","documento":"555","fechaNacimiento":"1980-01-01"},
                     "codigoApartamento":"BH-203","entrada":"%s","salida":"%s","horaEstimadaLlegada":"15:00"}"""
                    .formatted(entrada.plusDays(1), entrada.plusDays(4)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("NOCHES_NO_DISPONIBLES"));

            ejecutar(post("/api/reservas/" + id + "/confirmacion"), recepcion, null)
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.codigo").value("ANTICIPO_INSUFICIENTE"));

            String valor = leer(ejecutar(get("/api/reservas/" + id), recepcion, null), "$.cotizacion.valorTotal");
            long anticipo = Math.round(Double.parseDouble(valor) * 0.3);
            ejecutar(post("/api/reservas/" + id + "/folio/pagos"), recepcion,
                    "{\"valor\":%d,\"medio\":\"TRANSFERENCIA\"}".formatted(anticipo))
                    .andExpect(status().isOk());
            ejecutar(post("/api/reservas/" + id + "/confirmacion"), recepcion, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("CONFIRMADA"));

            ejecutar(post("/api/reservas/" + id + "/cancelacion"), laura, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("CANCELADA"))
                    .andExpect(jsonPath("$.saldo").value((int) -anticipo));

            ejecutar(post("/api/reservas/directas"), recepcion, """
                    {"titular":{"nombre":"Pedro Pérez","documento":"555","fechaNacimiento":"1980-01-01"},
                     "codigoApartamento":"BH-203","entrada":"%s","salida":"%s","horaEstimadaLlegada":"15:00"}"""
                    .formatted(entrada, entrada.plusDays(3)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.canal").value("DIRECTO"));
        }

        @Test
        @DisplayName("Llegada del día → salida: exige cerrar el folio y con saldo solo el administrador autoriza")
        void llegadaSalidaYEstadoOperativo() throws Exception {
            String recepcion = token(RECEPCION);
            String id = leer(ejecutar(get("/api/reservas/llegadas?fecha=" + hoy()), recepcion, null), "$[0].id");
            ejecutar(post("/api/reservas/" + id + "/llegada"), recepcion, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("EN_CURSO"));
            ejecutar(get("/api/apartamentos/BH-203"), null, null)
                    .andExpect(jsonPath("$.estadoOperativo").value("OCUPADO"));

            ejecutar(post("/api/reservas/" + id + "/salida"), recepcion, null)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("FOLIO_CON_SALDO"));
            ejecutar(post("/api/reservas/" + id + "/salida"), recepcion, "{\"motivoAutorizacion\":\"Cortesía\"}")
                    .andExpect(status().isForbidden());

            String saldo = leer(ejecutar(get("/api/reservas/" + id + "/folio"), recepcion, null), "$.saldo");
            ejecutar(post("/api/reservas/" + id + "/folio/pagos"), recepcion,
                    "{\"valor\":%s,\"medio\":\"EFECTIVO\"}".formatted(saldo)).andExpect(status().isOk());
            ejecutar(post("/api/reservas/" + id + "/salida"), recepcion, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("FINALIZADA"))
                    .andExpect(jsonPath("$.folioCerrado").value(true));

            ejecutar(get("/api/apartamentos/BH-203"), null, null)
                    .andExpect(jsonPath("$.estadoOperativo").value("PENDIENTE_PREPARACION"));
            String servicio = token(SERVICIO);
            ejecutar(put("/api/apartamentos/BH-203/estado-operativo"), servicio, "{\"estado\":\"FUERA_DE_SERVICIO\"}")
                    .andExpect(status().isForbidden());
            ejecutar(put("/api/apartamentos/BH-203/estado-operativo"), servicio, "{\"estado\":\"EN_PREPARACION\"}")
                    .andExpect(status().isOk());
            ejecutar(put("/api/apartamentos/BH-203/estado-operativo"), servicio, "{\"estado\":\"PREPARADO\"}")
                    .andExpect(jsonPath("$.estadoOperativo").value("PREPARADO"));
        }

        @Test
        @DisplayName("Los datos de prueba cubren los seis estados y los tres canales")
        void datosDePrueba() throws Exception {
            String recepcion = token(RECEPCION);
            for (String estado : new String[]{"PENDIENTE", "CONFIRMADA", "EN_CURSO", "FINALIZADA", "CANCELADA", "NO_SHOW"}) {
                ejecutar(get("/api/reservas?estado=" + estado), recepcion, null)
                        .andExpect(jsonPath("$.totalElementos", greaterThan(0)));
            }
            for (String canal : new String[]{"PORTAL", "DIRECTO", "EXTERNO"}) {
                ejecutar(get("/api/reservas?canal=" + canal), recepcion, null)
                        .andExpect(jsonPath("$.totalElementos", greaterThan(0)));
            }
            ejecutar(get("/api/apartamentos/BH-302/bloqueos"), recepcion, null)
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    // --- Administración ---

    @Nested
    @DisplayName("Administración")
    class Administracion {

        @Test
        @DisplayName("Un apartamento nuevo solo se activa con imágenes y tarifas en todas las temporadas")
        void activacionDeApartamento() throws Exception {
            String admin = token(ADMIN);
            ejecutar(post("/api/apartamentos"), admin, """
                    {"codigo":"BH-401","nombre":"Colibrí","descripcion":"Ático","dormitorios":1,"capacidad":2}""")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.activo").value(false));
            ejecutar(post("/api/apartamentos/BH-401/activacion"), admin, null)
                    .andExpect(jsonPath("$.codigo").value("IMAGENES_INVALIDAS"));
            ejecutar(put("/api/apartamentos/BH-401/imagenes"), admin, "[{\"url\":\"https://img/1.jpg\",\"principal\":true}]")
                    .andExpect(status().isOk());
            ejecutar(post("/api/apartamentos/BH-401/activacion"), admin, null)
                    .andExpect(jsonPath("$.codigo").value("TARIFAS_INCOMPLETAS"));
            for (String temporada : new String[]{"BASE", "MITAD_DE_ANIO", "SEMANA_SANTA", "FIN_DE_ANIO"}) {
                ejecutar(post("/api/tarifas"), admin, """
                        {"codigoApartamento":"BH-401","temporada":"%s","valor":110000}""".formatted(temporada))
                        .andExpect(status().isCreated());
            }
            ejecutar(post("/api/apartamentos/BH-401/activacion"), admin, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activo").value(true));
            ejecutar(post("/api/apartamentos"), token(RECEPCION), """
                    {"codigo":"BH-402","nombre":"X","dormitorios":1,"capacidad":2}""")
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Una temporada que se solapa con otra se rechaza")
        void temporadaSolapada() throws Exception {
            LocalDate inicio = LocalDate.of(hoy().getYear(), 12, 20);
            ejecutar(post("/api/temporadas"), token(ADMIN), """
                    {"nombre":"NAVIDAD","rangos":[{"inicio":"%s","fin":"%s"}],"estanciaMinimaNoches":1}"""
                    .formatted(inicio, inicio.plusDays(5)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("TEMPORADAS_SOLAPADAS"));
        }

        @Test
        @DisplayName("Publicar una política nueva no cambia la versión congelada en las reservas existentes")
        void versionadoDePolitica() throws Exception {
            String admin = token(ADMIN);
            String nueva = leer(ejecutar(post("/api/politicas"), admin, """
                    {"tramos":[{"diasAntelacionMinimos":15,"porcentajeRetencion":0},
                               {"diasAntelacionMinimos":7,"porcentajeRetencion":30},
                               {"diasAntelacionMinimos":0,"porcentajeRetencion":60}],
                     "porcentajeRetencionNoShow":30}""").andExpect(status().isCreated()), "$.version");
            assertThat(Integer.parseInt(nueva)).isGreaterThan(1);
            ejecutar(get("/api/reservas?estado=FINALIZADA"), token(RECEPCION), null)
                    .andExpect(jsonPath("$.contenido[0].id").exists());
            String id = leer(ejecutar(get("/api/reservas?estado=FINALIZADA"), token(RECEPCION), null), "$.contenido[0].id");
            ejecutar(get("/api/reservas/" + id), admin, null).andExpect(jsonPath("$.versionPolitica").value(1));
        }
    }
}
