package co.edu.uniquindio.sga.infrastructure.rest;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Corte 4: canal externo, reportes, recordatorios, IA e imágenes con las alternativas
 * locales (I-08). Usa su propia base en memoria para conocer la credencial del canal.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sga-integraciones",
        "sga.datos-prueba.credencial-canal=" + IntegracionesApiTest.CREDENCIAL,
        "sga.reservas.intervalo-vencimiento-ms=3600000",
        "sga.ia.habilitada=true",
        "sga.ia.proveedor=simulado",
        "sga.imagenes.proveedor=local",
        "sga.imagenes.directorio=build/imagenes-prueba"})
@AutoConfigureMockMvc
class IntegracionesApiTest {

    static final String CREDENCIAL = "credencial-de-prueba-0123456789";
    private static final String CLAVE = "Bahareque2026";

    @Autowired
    private MockMvc mvc;

    private static LocalDate hoy() {
        return LocalDate.now(ZoneId.of("America/Bogota"));
    }

    private String token(String correo) throws Exception {
        String cuerpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, CLAVE)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.token");
    }

    private String admin() throws Exception {
        return token("admin@aldeabahareque.co");
    }

    private ResultActions ejecutar(MockHttpServletRequestBuilder solicitud, String token, String json) throws Exception {
        if (token != null) solicitud.header("Authorization", "Bearer " + token);
        if (json != null) solicitud.contentType(MediaType.APPLICATION_JSON).content(json);
        return mvc.perform(solicitud);
    }

    private ResultActions canal(MockHttpServletRequestBuilder solicitud, String json) throws Exception {
        return canal(solicitud, "ViajaEje", CREDENCIAL, json);
    }

    private ResultActions canal(MockHttpServletRequestBuilder solicitud, String codigo, String credencial, String json)
            throws Exception {
        solicitud.header("X-Canal", codigo).header("X-Credencial-Canal", credencial);
        if (json != null) solicitud.contentType(MediaType.APPLICATION_JSON).content(json);
        return mvc.perform(solicitud);
    }

    private static String reservaExterna(String idExterno, String apartamento, LocalDate entrada, LocalDate salida) {
        return """
                {"idExterno":"%s","codigoApartamento":"%s","entrada":"%s","salida":"%s","horaEstimadaLlegada":"15:30",
                 "titular":{"nombre":"Oliver Smith","documento":"P-123456","correo":"oliver@mail.com",
                            "telefono":"+44 20 0000","fechaNacimiento":"1985-03-02"},
                 "acompanantes":[{"nombre":"Emma Smith","fechaNacimiento":"1987-07-09"}]}"""
                .formatted(idExterno, apartamento, entrada, salida);
    }

    private static String leer(ResultActions resultado, String ruta) throws Exception {
        return JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), ruta).toString();
    }

    @Nested
    @DisplayName("Canal externo (10.3.1)")
    class CanalExterno {

        @Test
        @DisplayName("Sin credencial o con una credencial errada se responde 401 y queda en la bitácora")
        void credencialInvalida() throws Exception {
            canal(get("/api/canal/disponibilidad").param("entrada", hoy().plusDays(40).toString())
                    .param("salida", hoy().plusDays(42).toString()), "ViajaEje", "errada", null)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.codigo").value("CREDENCIAL_CANAL_INVALIDA"));
            mvc.perform(get("/api/canal/disponibilidad").param("entrada", hoy().plusDays(40).toString())
                    .param("salida", hoy().plusDays(42).toString())).andExpect(status().isUnauthorized());

            ejecutar(get("/api/canales/bitacora").param("canal", "ViajaEje"), admin(), null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contenido[*].tipo", hasItem("CREDENCIAL_INVALIDA")));
        }

        @Test
        @DisplayName("Consulta de disponibilidad para un rango y una composición de grupo")
        void disponibilidad() throws Exception {
            canal(get("/api/canal/disponibilidad").param("entrada", hoy().plusDays(40).toString())
                    .param("salida", hoy().plusDays(42).toString()).param("personas", "4"), null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.canal").value("ViajaEje"))
                    .andExpect(jsonPath("$.disponibles[*].codigoApartamento", hasItems("BH-201", "BH-301")))
                    .andExpect(jsonPath("$.disponibles[*].codigoApartamento", not(hasItem("BH-101"))));
        }

        @Test
        @DisplayName("Reserva aceptada (201) y RN-19: el mensaje repetido devuelve la misma reserva sin duplicarla (200)")
        void aceptadaYRepetida() throws Exception {
            String cuerpo = reservaExterna("VE-5001", "BH-203", hoy().plusDays(50), hoy().plusDays(53));
            ResultActions primera = canal(post("/api/canal/reservas"), cuerpo)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.resultado").value("ACEPTADA"))
                    .andExpect(jsonPath("$.reserva.canal").value("EXTERNO"))
                    .andExpect(jsonPath("$.reserva.estado").value("PENDIENTE"));
            String id = leer(primera, "$.reserva.id");

            canal(post("/api/canal/reservas"), cuerpo)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultado").value("DUPLICADA"))
                    .andExpect(jsonPath("$.reserva.id").value(id));

            ejecutar(get("/api/reservas").param("canal", "EXTERNO").param("titular", "Oliver"), admin(), null)
                    .andExpect(jsonPath("$.contenido[?(@.id == '%s')]".formatted(id), hasSize(1)));
        }

        @Test
        @DisplayName("RN-18: la reserva en conflicto se rechaza (409), se registra y la existente no cambia")
        void conflicto() throws Exception {
            String token = admin();
            canal(post("/api/canal/reservas"), reservaExterna("VE-6001", "BH-201", hoy().plusDays(60), hoy().plusDays(63)))
                    .andExpect(status().isCreated());

            canal(post("/api/canal/reservas"), reservaExterna("VE-6002", "BH-201", hoy().plusDays(61), hoy().plusDays(64)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.resultado").value("RECHAZADA_CONFLICTO"))
                    .andExpect(jsonPath("$.conflicto.motivo").value("NOCHES_NO_DISPONIBLES"))
                    .andExpect(jsonPath("$.reserva").doesNotExist());

            ResultActions pendientes = ejecutar(get("/api/canales/conflictos").param("pendientes", "true"), token, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contenido[*].idExterno", hasItem("VE-6002")));
            String idConflicto = JsonPath.<java.util.List<String>>read(pendientes.andReturn().getResponse()
                    .getContentAsString(), "$.contenido[?(@.idExterno == 'VE-6002')].id").getFirst();

            ejecutar(post("/api/canales/conflictos/" + idConflicto + "/revision"), token,
                    "{\"nota\":\"Se ofreció BH-202 al canal\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.revisado").value(true))
                    .andExpect(jsonPath("$.revisadoPor").value("Valentina Arango"));
            ejecutar(post("/api/canales/conflictos/" + idConflicto + "/revision"), token, "{\"nota\":\"Otra\"}")
                    .andExpect(status().isConflict());

            canal(post("/api/canal/reservas"), reservaExterna("VE-6001", "BH-201", hoy().plusDays(60), hoy().plusDays(63)))
                    .andExpect(jsonPath("$.reserva.salida").value(hoy().plusDays(63).toString()));
        }

        @Test
        @DisplayName("Cancelación de una reserva externa; repetir el mensaje no falla")
        void cancelacion() throws Exception {
            canal(post("/api/canal/reservas"), reservaExterna("VE-7001", "BH-301", hoy().plusDays(70), hoy().plusDays(72)))
                    .andExpect(status().isCreated());

            canal(delete("/api/canal/reservas/VE-7001"), null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultado").value("CANCELADA"))
                    .andExpect(jsonPath("$.reserva.estado").value("CANCELADA"));
            canal(delete("/api/canal/reservas/VE-7001"), null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultado").value("YA_CANCELADA"));
            canal(delete("/api/canal/reservas/NO-EXISTE"), null).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("El administrador registra canales; la credencial se muestra una vez y un canal inactivo no opera")
        void administracionDeCanales() throws Exception {
            String token = admin();
            ResultActions registro = ejecutar(post("/api/canales"), token,
                    "{\"codigo\":\"Turismo-QX\",\"nombre\":\"Turismo Quindío\",\"tipo\":\"METABUSCADOR\"}")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.credencial", hasLength(48)));
            String credencial = leer(registro, "$.credencial");
            ejecutar(post("/api/canales"), token,
                    "{\"codigo\":\"Turismo-QX\",\"nombre\":\"Otro\",\"tipo\":\"METABUSCADOR\"}").andExpect(status().isConflict());

            MockHttpServletRequestBuilder consulta = get("/api/canal/disponibilidad")
                    .param("entrada", hoy().plusDays(80).toString()).param("salida", hoy().plusDays(81).toString());
            canal(consulta, "Turismo-QX", credencial, null).andExpect(status().isOk());

            ejecutar(put("/api/canales/Turismo-QX/activo"), token, "{\"activo\":false}")
                    .andExpect(jsonPath("$.activo").value(false));
            canal(get("/api/canal/disponibilidad").param("entrada", hoy().plusDays(80).toString())
                    .param("salida", hoy().plusDays(81).toString()), "Turismo-QX", credencial, null)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("CANAL_INACTIVO"));
            ejecutar(get("/api/canales"), token, null)
                    .andExpect(jsonPath("$[*].codigo", hasItems("ViajaEje", "RutaCafetera", "Turismo-QX")))
                    .andExpect(jsonPath("$[0].credencialCifrada").doesNotExist());
            ejecutar(get("/api/canales"), token("laura.gomez@correo.com"), null).andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Reportes (7.9)")
    class ReportesApi {

        @Test
        @DisplayName("Ocupación, ingresos, reservas por canal e indicadores con filtro de fechas")
        void reportes() throws Exception {
            String token = admin();
            String desde = hoy().minusDays(30).toString();
            String hasta = hoy().plusDays(30).toString();
            ejecutar(get("/api/reportes/ocupacion").param("desde", desde).param("hasta", hasta), token, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.apartamentosActivos").value(7))
                    .andExpect(jsonPath("$.nochesDisponibles").value(7 * 61))
                    .andExpect(jsonPath("$.nochesVendidas", greaterThan(0)));
            ejecutar(get("/api/reportes/ingresos").param("desde", desde).param("hasta", hasta), token, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.porMedio[*].medio", hasItems("EFECTIVO", "TRANSFERENCIA", "TARJETA")))
                    .andExpect(jsonPath("$.total", greaterThan(0)));
            ejecutar(get("/api/reportes/canales").param("desde", desde).param("hasta", hasta), token, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.canales[*].canal", contains("PORTAL", "DIRECTO", "EXTERNO")))
                    .andExpect(jsonPath("$.canales[?(@.canal == 'DIRECTO')].porEstado.NO_SHOW", contains(1)));
            ejecutar(get("/api/reportes/indicadores").param("desde", desde).param("hasta", hasta), token, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tasaNoShow", greaterThan(0.0)));
        }

        @Test
        @DisplayName("Exporta a CSV; rechaza periodos inválidos y usuarios que no son administradores")
        void csvYValidaciones() throws Exception {
            String token = admin();
            ejecutar(get("/api/reportes/ingresos/csv").param("desde", hoy().minusDays(30).toString())
                    .param("hasta", hoy().toString()), token, null)
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", containsString("attachment")))
                    .andExpect(content().string(containsString("Medio de pago;Pagos;Total (COP)")));
            ejecutar(get("/api/reportes/ocupacion").param("desde", hoy().toString())
                    .param("hasta", hoy().minusDays(1).toString()), token, null)
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.codigo").value("PERIODO_INVALIDO"));
            ejecutar(get("/api/reportes/ocupacion").param("desde", hoy().toString()).param("hasta", hoy().toString()),
                    token("recepcion@aldeabahareque.co"), null).andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Recordatorios, IA e imágenes")
    class OtrasIntegraciones {

        @Test
        @DisplayName("Los recordatorios del día se envían una sola vez por reserva")
        void recordatorios() throws Exception {
            String token = admin();
            ResultActions directa = ejecutar(post("/api/reservas/directas"), token("recepcion@aldeabahareque.co"), """
                    {"titular":{"nombre":"Rosa Ocampo","documento":"41888999","correo":"rosa.ocampo@correo.com",
                                "telefono":"3150001122","fechaNacimiento":"1970-02-11"},
                     "codigoApartamento":"BH-301","entrada":"%s","salida":"%s","horaEstimadaLlegada":"14:00"}"""
                    .formatted(hoy().plusDays(1), hoy().plusDays(3))).andExpect(status().isCreated());
            String id = leer(directa, "$.id");
            ejecutar(post("/api/reservas/" + id + "/folio/pagos"), token,
                    "{\"valor\":%s,\"medio\":\"EFECTIVO\"}".formatted(leer(directa, "$.saldo")))
                    .andExpect(status().isOk());
            ejecutar(post("/api/reservas/" + id + "/confirmacion"), token, null).andExpect(status().isOk());

            ejecutar(post("/api/recordatorios/envios"), token, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.idReserva == '%s')].tipo".formatted(id), contains("LLEGADA")));
            ejecutar(post("/api/recordatorios/envios"), token, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
            ejecutar(get("/api/recordatorios"), token, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contenido[*].destinatario", hasItem("rosa.ocampo@correo.com")));
        }

        @Test
        @DisplayName("La IA sugiere la gravedad de una novedad; registrar la novedad no depende de ella")
        void sugerenciaGravedad() throws Exception {
            String token = token("servicio@aldeabahareque.co");
            ejecutar(get("/api/novedades/ia"), token, null).andExpect(jsonPath("$.habilitada").value(true));
            ejecutar(post("/api/novedades/sugerencia-gravedad"), token, "{\"descripcion\":\"Hay olor a gas en la cocina\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.disponible").value(true))
                    .andExpect(jsonPath("$.gravedad").value("CRITICA"));
            ejecutar(post("/api/apartamentos/BH-203/novedades"), token,
                    "{\"descripcion\":\"Hay olor a gas en la cocina\",\"gravedad\":\"CRITICA\"}")
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("El administrador sube una imagen al almacén local y queda publicada")
        void imagenes() throws Exception {
            byte[] png = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};
            ResultActions subida = mvc.perform(multipart("/api/imagenes")
                            .file(new MockMultipartFile("archivo", "balcon.png", "image/png", png))
                            .header("Authorization", "Bearer " + admin()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.url", startsWith("/archivos/imagenes/")));
            mvc.perform(get(leer(subida, "$.url"))).andExpect(status().isOk());

            mvc.perform(multipart("/api/imagenes")
                            .file(new MockMultipartFile("archivo", "animada.gif", "image/gif", png))
                            .header("Authorization", "Bearer " + admin()))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.codigo").value("IMAGEN_INVALIDA"));
        }
    }
}
