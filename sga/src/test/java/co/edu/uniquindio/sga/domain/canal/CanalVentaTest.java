package co.edu.uniquindio.sga.domain.canal;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.reserva.ReferenciaExterna;
import co.edu.uniquindio.sga.domain.usuario.CifradorContrasenas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static co.edu.uniquindio.sga.domain.Escenario.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class CanalVentaTest {

    private static final CifradorContrasenas CIFRADOR = new CifradorContrasenas() {
        public String cifrar(String texto) { return "cifrado:" + texto; }
        public boolean coincide(String texto, String cifrado) { return cifrado.equals("cifrado:" + texto); }
    };

    @Test
    @DisplayName("Al registrar un canal se genera una credencial aleatoria que se guarda cifrada")
    void registro() {
        CanalVenta.Credencial registro = CanalVenta.registrar("ViajaEje", "ViajaEje", TipoCanal.AGENCIA_EN_LINEA, CIFRADOR);

        assertThat(registro.credencialEnClaro()).hasSize(48).matches("[0-9a-f]+");
        assertThat(registro.canal().getCredencialCifrada()).isNotEqualTo(registro.credencialEnClaro());
        assertThat(registro.canal().isActivo()).isTrue();
        assertThatCode(() -> registro.canal().autenticar(registro.credencialEnClaro(), CIFRADOR)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Una credencial incorrecta o ausente se rechaza")
    void credencialIncorrecta() {
        CanalVenta canal = CanalVenta.registrar("ViajaEje", "ViajaEje", TipoCanal.AGENCIA_EN_LINEA, CIFRADOR).canal();

        violaRegla(CodigoError.CREDENCIAL_CANAL_INVALIDA, () -> canal.autenticar("otra", CIFRADOR));
        violaRegla(CodigoError.CREDENCIAL_CANAL_INVALIDA, () -> canal.autenticar(null, CIFRADOR));
    }

    @Test
    @DisplayName("Un canal inactivo no opera aunque presente su credencial correcta")
    void canalInactivo() {
        CanalVenta.Credencial registro = CanalVenta.registrar("ViajaEje", "ViajaEje", TipoCanal.AGENCIA_EN_LINEA, CIFRADOR);
        registro.canal().desactivar();

        violaRegla(CodigoError.CANAL_INACTIVO, () -> registro.canal().autenticar(registro.credencialEnClaro(), CIFRADOR));

        registro.canal().activar();
        assertThatCode(() -> registro.canal().autenticar(registro.credencialEnClaro(), CIFRADOR)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Renovar la credencial invalida la anterior")
    void renovarCredencial() {
        CanalVenta.Credencial registro = CanalVenta.registrar("ViajaEje", "ViajaEje", TipoCanal.AGENCIA_EN_LINEA, CIFRADOR);
        String nueva = registro.canal().renovarCredencial(CIFRADOR);

        assertThat(nueva).isNotEqualTo(registro.credencialEnClaro());
        violaRegla(CodigoError.CREDENCIAL_CANAL_INVALIDA,
                () -> registro.canal().autenticar(registro.credencialEnClaro(), CIFRADOR));
        assertThatCode(() -> registro.canal().autenticar(nueva, CIFRADOR)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("El código del canal solo admite letras, números, guion y guion bajo")
    void codigoInvalido() {
        violaRegla(CodigoError.CANAL_INVALIDO,
                () -> CanalVenta.registrar("viaja eje", "ViajaEje", TipoCanal.AGENCIA_EN_LINEA, CIFRADOR));
        violaRegla(CodigoError.CANAL_INVALIDO,
                () -> CanalVenta.registrar("ViajaEje", " ", TipoCanal.AGENCIA_EN_LINEA, CIFRADOR));
        violaRegla(CodigoError.CANAL_INVALIDO,
                () -> CanalVenta.registrarConCredencial("ViajaEje", "ViajaEje", TipoCanal.METABUSCADOR, "corta", CIFRADOR));
    }

    @Test
    @DisplayName("RN-18: el conflicto queda pendiente hasta que el administrador lo revisa, y se revisa una sola vez")
    void revisionDeConflicto() {
        ConflictoCanal conflicto = new ConflictoCanal("c-1", new ReferenciaExterna("ViajaEje", "VE-9"), "BH-101",
                noviembre(10, 12), "Oliver Smith", CodigoError.NOCHES_NO_DISPONIBLES, AHORA, null);
        assertThat(conflicto.estaPendiente()).isTrue();

        ConflictoCanal revisado = conflicto.revisar("Valentina Arango", "Se ofreció BH-202 al canal", AHORA.plusHours(1));

        assertThat(revisado.estaPendiente()).isFalse();
        assertThat(revisado.revision().autor()).isEqualTo("Valentina Arango");
        violaRegla(CodigoError.CONFLICTO_YA_REVISADO, () -> revisado.revisar("Otro", "Nota", AHORA.plusHours(2)));
        violaRegla(CodigoError.AUTORIZACION_INVALIDA, () -> conflicto.revisar("Valentina Arango", " ", AHORA));
    }
}
