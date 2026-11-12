package co.edu.uniquindio.sga.infrastructure.rest;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 10.2: el sistema opera con la IA deshabilitada por configuración, sin tocar código. */
@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:sga-sin-ia", "sga.ia.habilitada=false",
        "sga.reservas.intervalo-vencimiento-ms=3600000"})
@AutoConfigureMockMvc
class IaDeshabilitadaTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("Sin IA la sugerencia se degrada de forma visible y la novedad se registra igual")
    void sinIa() throws Exception {
        String cuerpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"servicio@aldeabahareque.co\",\"contrasena\":\"Bahareque2026\"}"))
                .andReturn().getResponse().getContentAsString();
        String token = "Bearer " + JsonPath.read(cuerpo, "$.token");

        mvc.perform(get("/api/novedades/ia").header("Authorization", token))
                .andExpect(jsonPath("$.habilitada").value(false));
        mvc.perform(post("/api/novedades/sugerencia-gravedad").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"descripcion\":\"Gotera en el baño\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(false))
                .andExpect(jsonPath("$.mensaje").value("La IA está deshabilitada. Elija la gravedad manualmente."));
        mvc.perform(post("/api/apartamentos/BH-201/novedades").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descripcion\":\"Gotera en el baño\",\"gravedad\":\"ALTA\"}"))
                .andExpect(status().isCreated());
    }
}
