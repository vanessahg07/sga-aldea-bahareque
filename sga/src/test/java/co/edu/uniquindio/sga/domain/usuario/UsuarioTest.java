package co.edu.uniquindio.sga.domain.usuario;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static co.edu.uniquindio.sga.domain.Escenario.violaRegla;
import static org.assertj.core.api.Assertions.assertThat;

class UsuarioTest {

    /** Cifrador de prueba: reversible y predecible, suficiente para verificar las reglas. */
    static final CifradorContrasenas CIFRADOR = new CifradorContrasenas() {
        public String cifrar(String texto) { return "cifrado:" + texto; }
        public boolean coincide(String texto, String cifrado) { return cifrado.equals("cifrado:" + texto); }
    };

    private Usuario huesped() {
        return Usuario.crear("Laura Gómez", "Laura@Correo.com", "Clave1234", "3001234567", "1094000111",
                LocalDate.of(1990, 5, 10), Rol.HUESPED, CIFRADOR);
    }

    @Test
    @DisplayName("El registro guarda la contraseña cifrada y el correo normalizado")
    void registro() {
        Usuario usuario = huesped();
        assertThat(usuario.getContrasenaCifrada()).isNotEqualTo("Clave1234");
        assertThat(usuario.getCorreo()).isEqualTo("laura@correo.com");
        assertThat(usuario.autenticar("Clave1234", CIFRADOR)).isTrue();
        assertThat(usuario.autenticar("otra", CIFRADOR)).isFalse();
    }

    @ParameterizedTest(name = "\"{0}\" se rechaza")
    @ValueSource(strings = {"Corta1", "sinmayuscula1", "SinNumeroAqui", ""})
    @DisplayName("La contraseña exige 8 caracteres, una mayúscula y un número")
    void contrasenaInsegura(String contrasena) {
        violaRegla(CodigoError.CONTRASENA_INSEGURA, () -> Usuario.crear("Ana", "ana@correo.com", contrasena,
                "300", "1", LocalDate.of(1990, 1, 1), Rol.HUESPED, CIFRADOR));
    }

    @ParameterizedTest(name = "\"{0}\" se rechaza")
    @ValueSource(strings = {"sin-arroba.com", "a@b", "a b@correo.com"})
    @DisplayName("El correo debe tener formato válido")
    void correoInvalido(String correo) {
        violaRegla(CodigoError.CORREO_INVALIDO, () -> Usuario.crear("Ana", correo, "Clave1234", "300", "1",
                LocalDate.of(1990, 1, 1), Rol.HUESPED, CIFRADOR));
    }

    @Test
    @DisplayName("Cambiar la contraseña exige la actual")
    void cambioDeContrasena() {
        Usuario usuario = huesped();
        violaRegla(CodigoError.CONTRASENA_ACTUAL_INCORRECTA,
                () -> usuario.cambiarContrasena("Equivocada1", "NuevaClave9", CIFRADOR));
        usuario.cambiarContrasena("Clave1234", "NuevaClave9", CIFRADOR);
        assertThat(usuario.autenticar("NuevaClave9", CIFRADOR)).isTrue();
    }

    @Test
    @DisplayName("No se elimina un usuario titular de reservas activas; la eliminación es lógica")
    void eliminacion() {
        Usuario usuario = huesped();
        violaRegla(CodigoError.USUARIO_CON_RESERVAS_ACTIVAS, () -> usuario.eliminar(true));
        usuario.eliminar(false);
        assertThat(usuario.isEliminado()).isTrue();
        assertThat(usuario.autenticar("Clave1234", CIFRADOR)).isFalse();
    }

    @Test
    @DisplayName("El código de recuperación sirve una sola vez")
    void codigoDeUnSoloUso() {
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 1, 10, 0);
        CodigoRecuperacion.Emision emision = CodigoRecuperacion.emitir("laura@correo.com", ahora, CIFRADOR);
        assertThat(emision.codigoEnClaro()).hasSize(6);

        emision.codigo().usar(emision.codigoEnClaro(), ahora.plusMinutes(5), CIFRADOR);
        violaRegla(CodigoError.CODIGO_RECUPERACION_INVALIDO,
                () -> emision.codigo().usar(emision.codigoEnClaro(), ahora.plusMinutes(6), CIFRADOR));
    }

    @Test
    @DisplayName("El código de recuperación vence a los 15 minutos")
    void codigoVencido() {
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 1, 10, 0);
        CodigoRecuperacion.Emision emision = CodigoRecuperacion.emitir("laura@correo.com", ahora, CIFRADOR);
        violaRegla(CodigoError.CODIGO_RECUPERACION_INVALIDO,
                () -> emision.codigo().usar(emision.codigoEnClaro(), ahora.plusMinutes(16), CIFRADOR));
    }

    @Test
    @DisplayName("Un código equivocado se rechaza")
    void codigoEquivocado() {
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 1, 10, 0);
        CodigoRecuperacion.Emision emision = CodigoRecuperacion.emitir("laura@correo.com", ahora, CIFRADOR);
        String otro = emision.codigoEnClaro().equals("000000") ? "111111" : "000000";
        violaRegla(CodigoError.CODIGO_RECUPERACION_INVALIDO, () -> emision.codigo().usar(otro, ahora, CIFRADOR));
    }
}
