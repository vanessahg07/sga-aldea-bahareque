package co.edu.uniquindio.sga.infrastructure.seguridad;

import co.edu.uniquindio.sga.infrastructure.rest.comun.ErrorRespuesta;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;

/**
 * Seguridad sin estado con JWT. Cada endpoint se protege por rol aquí y en los
 * controladores; la restricción por propietario del recurso se aplica en los casos de uso.
 */
@Configuration
@EnableMethodSecurity
public class ConfiguracionSeguridad {

    private static final JsonMapper JSON = JsonMapper.builder().findAndAddModules().build();

    @Bean
    public SecurityFilterChain cadenaSeguridad(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> { })
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        // El canal externo se autentica con su propia credencial (encabezados), no con JWT.
                        .requestMatchers("/api/canal/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/apartamentos/gestion",
                                "/api/apartamentos/pendientes-preparacion").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/alojamiento", "/api/politicas/vigente",
                                "/api/temporadas").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/apartamentos", "/api/apartamentos/{codigo}",
                                "/api/apartamentos/{codigo}/calendario", "/api/apartamentos/{codigo}/tarifas").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/disponibilidad/busqueda", "/api/cotizaciones").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        // Fuera de /api solo hay archivos públicos: imágenes subidas y el frontend compilado.
                        .anyRequest().permitAll())
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(convertidorRoles())))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) ->
                                escribir(res, HttpServletResponse.SC_UNAUTHORIZED, "NO_AUTENTICADO",
                                        "Debe iniciar sesión para acceder a este recurso"))
                        .accessDeniedHandler((req, res, ex) ->
                                escribir(res, HttpServletResponse.SC_FORBIDDEN, "ACCESO_DENEGADO",
                                        "No tiene permiso para acceder a este recurso")));
        return http.build();
    }

    /** Los roles viajan en el claim "rol" y se exponen como ROLE_&lt;ROL&gt;. */
    private JwtAuthenticationConverter convertidorRoles() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName(EmisorTokens.CLAIM_ROL);
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(roles);
        return convertidor;
    }

    private static void escribir(HttpServletResponse respuesta, int estado, String codigo, String mensaje)
            throws IOException {
        respuesta.setStatus(estado);
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");
        JSON.writeValue(respuesta.getOutputStream(), ErrorRespuesta.de(codigo, mensaje, List.of()));
    }

    @Bean
    public JwtEncoder jwtEncoder(ClaveJwt clave) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave.clave()));
    }

    @Bean
    public JwtDecoder jwtDecoder(ClaveJwt clave) {
        return NimbusJwtDecoder.withSecretKey(clave.clave()).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * En desarrollo el frontend de React se sirve aparte (Vite en el puerto 5173). En el
     * despliegue lo entrega este mismo servidor, así que no hay peticiones de otro origen.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", cors);
        return fuente;
    }
}
