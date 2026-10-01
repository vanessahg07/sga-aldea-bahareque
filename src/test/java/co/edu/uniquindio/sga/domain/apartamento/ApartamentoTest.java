package co.edu.uniquindio.sga.domain.apartamento;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import co.edu.uniquindio.sga.domain.comun.RangoFechas;
import co.edu.uniquindio.sga.domain.tarifa.TablaTarifas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static co.edu.uniquindio.sga.domain.Escenario.*;
import static co.edu.uniquindio.sga.domain.apartamento.EstadoOperativo.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ApartamentoTest {

    private static final RangoFechas NOCHES_RESERVADAS =
            new RangoFechas(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 12));

    private static Apartamento nuevoSinImagenes() {
        return Apartamento.crear("BH-999", "Nuevo", "", 1, 2, Set.of(), false);
    }

    /** Apartamento que acaba de despedir a un grupo: queda PENDIENTE_PREPARACION. */
    private static Apartamento recienLiberado() {
        Apartamento apartamento = apartamento("BH-201");
        apartamento.recibirGrupo();
        apartamento.liberar();
        return apartamento;
    }

    // --- Capacidad ---

    @Test
    @DisplayName("RN-02: ocupantes dentro de la capacidad se aceptan")
    void capacidadSuficiente() {
        // Arrange
        Apartamento cafetal = apartamento("BH-201");

        // Act
        Throwable error = catchThrowable(() -> cafetal.validarCapacidad(4));

        // Assert
        assertThat(error).isNull();
    }

    @Test
    @DisplayName("RN-02 violada: un ocupante más que la capacidad se rechaza")
    void capacidadExcedida() {
        // Arrange
        Apartamento cafetal = apartamento("BH-201");

        // Act
        ExcepcionNegocio error = capturar(() -> cafetal.validarCapacidad(5));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.CAPACIDAD_EXCEDIDA);
    }

    // --- Activación ---

    @Test
    @DisplayName("No se activa un apartamento sin tarifas en todas las temporadas")
    void noSeActivaSinTarifasCompletas() {
        // Arrange
        Apartamento nuevo = nuevoSinImagenes();
        nuevo.definirImagenes(List.of(new ImagenApartamento("https://img/1.jpg", true)));
        TablaTarifas sinTarifas = new TablaTarifas(List.of());

        // Act
        ExcepcionNegocio error = capturar(() -> nuevo.activar(sinTarifas, calendario()));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.TARIFAS_INCOMPLETAS);
        assertThat(nuevo.estaALaVenta()).isFalse();
    }

    @Test
    @DisplayName("No se activa un apartamento sin imágenes")
    void noSeActivaSinImagenes() {
        // Arrange
        Apartamento nuevo = nuevoSinImagenes();

        // Act
        ExcepcionNegocio error = capturar(() -> nuevo.activar(tarifas(), calendario()));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.IMAGENES_INVALIDAS);
    }

    // --- Imágenes ---

    @Test
    @DisplayName("Imágenes: se admiten máximo 10")
    void masDeDiezImagenes() {
        // Arrange
        Apartamento apto = apartamento("BH-101");
        List<ImagenApartamento> once = IntStream.range(0, 11)
                .mapToObj(i -> new ImagenApartamento("https://img/" + i + ".jpg", i == 0)).toList();

        // Act
        ExcepcionNegocio error = capturar(() -> apto.definirImagenes(once));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.IMAGENES_INVALIDAS);
    }

    @Test
    @DisplayName("Imágenes: debe haber una principal")
    void sinImagenPrincipal() {
        // Arrange
        Apartamento apto = apartamento("BH-101");
        List<ImagenApartamento> sinPrincipal = List.of(
                new ImagenApartamento("https://img/a.jpg", false), new ImagenApartamento("https://img/b.jpg", false));

        // Act
        ExcepcionNegocio error = capturar(() -> apto.definirImagenes(sinPrincipal));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.IMAGENES_INVALIDAS);
    }

    @Test
    @DisplayName("Imágenes: no puede haber dos principales")
    void dosImagenesPrincipales() {
        // Arrange
        Apartamento apto = apartamento("BH-101");
        List<ImagenApartamento> dosPrincipales = List.of(
                new ImagenApartamento("https://img/a.jpg", true), new ImagenApartamento("https://img/b.jpg", true));

        // Act
        ExcepcionNegocio error = capturar(() -> apto.definirImagenes(dosPrincipales));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.IMAGENES_INVALIDAS);
    }

    @Test
    @DisplayName("Imágenes: la principal es la marcada, sin importar el orden")
    void imagenPrincipal() {
        // Arrange
        Apartamento apto = apartamento("BH-101");
        apto.definirImagenes(List.of(new ImagenApartamento("https://img/a.jpg", false),
                new ImagenApartamento("https://img/b.jpg", true)));

        // Act
        Optional<ImagenApartamento> principal = apto.imagenPrincipal();

        // Assert
        assertThat(principal).map(ImagenApartamento::url).contains("https://img/b.jpg");
    }

    // --- Estado operativo ---

    @Test
    @DisplayName("El administrador rehabilita un apartamento fuera de servicio hacia preparación")
    void administradorRehabilita() {
        // Arrange
        Apartamento apto = apartamento("BH-201");
        apto.cambiarEstadoPorAdministrador(FUERA_DE_SERVICIO);

        // Act
        apto.cambiarEstadoPorAdministrador(PENDIENTE_PREPARACION);

        // Assert
        assertThat(apto.getEstadoOperativo()).isEqualTo(PENDIENTE_PREPARACION);
    }

    @Test
    @DisplayName("El administrador nunca marca OCUPADO a mano")
    void administradorNoMarcaOcupado() {
        // Arrange
        Apartamento apto = apartamento("BH-201");

        // Act
        ExcepcionNegocio error = capturar(() -> apto.cambiarEstadoPorAdministrador(OCUPADO));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.TRANSICION_ESTADO_OPERATIVO_INVALIDA);
    }

    @Test
    @DisplayName("No se elimina un apartamento con reservas activas o futuras")
    void noSeEliminaConReservasActivas() {
        // Arrange
        Apartamento apartamento = apartamento("BH-101");

        // Act
        ExcepcionNegocio error = capturar(() -> apartamento.eliminar(true));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.APARTAMENTO_CON_RESERVAS_ACTIVAS);
        assertThat(apartamento.isEliminado()).isFalse();
    }

    @Test
    @DisplayName("La eliminación es lógica y saca el apartamento de la venta")
    void eliminacionLogica() {
        // Arrange
        Apartamento apartamento = apartamento("BH-101");

        // Act
        apartamento.eliminar(false);

        // Assert
        assertThat(apartamento.isEliminado()).isTrue();
        assertThat(apartamento.estaALaVenta()).isFalse();
    }

    @Test
    @DisplayName("Registrar la llegada deja el apartamento OCUPADO")
    void recibirGrupoLoOcupa() {
        // Arrange
        Apartamento apartamento = apartamento("BH-201");

        // Act
        apartamento.recibirGrupo();

        // Assert
        assertThat(apartamento.getEstadoOperativo()).isEqualTo(OCUPADO);
    }

    @Test
    @DisplayName("La salida deja el apartamento PENDIENTE_PREPARACION")
    void liberarLoDejaPendienteDePreparacion() {
        // Arrange
        Apartamento apartamento = apartamento("BH-201");
        apartamento.recibirGrupo();

        // Act
        apartamento.liberar();

        // Assert
        assertThat(apartamento.getEstadoOperativo()).isEqualTo(PENDIENTE_PREPARACION);
    }

    @Test
    @DisplayName("El personal de servicio completa la preparación y el apartamento puede recibir otro grupo")
    void cicloDePreparacion() {
        // Arrange
        Apartamento apartamento = recienLiberado();
        apartamento.cambiarEstadoPorPersonalDeServicio(EN_PREPARACION);

        // Act
        apartamento.cambiarEstadoPorPersonalDeServicio(PREPARADO);

        // Assert
        assertThat(apartamento.puedeRecibirGrupo()).isTrue();
    }

    @Test
    @DisplayName("RN-11 violada: no recibe un grupo si no está PREPARADO")
    void noRecibeGrupoSinPreparar() {
        // Arrange
        Apartamento apartamento = recienLiberado();

        // Act
        ExcepcionNegocio error = capturar(apartamento::recibirGrupo);

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.APARTAMENTO_NO_PREPARADO);
    }

    @Test
    @DisplayName("No se salta la preparación: PENDIENTE_PREPARACION → PREPARADO se rechaza")
    void noSeSaltaLaPreparacion() {
        // Arrange
        Apartamento apartamento = recienLiberado();

        // Act
        ExcepcionNegocio error = capturar(apartamento::terminarPreparacion);

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.TRANSICION_ESTADO_OPERATIVO_INVALIDA);
    }

    @Test
    @DisplayName("El personal de servicio no puede declarar FUERA_DE_SERVICIO")
    void personalDeServicioNoDeclaraFueraDeServicio() {
        // Arrange
        Apartamento apartamento = apartamento("BH-201");

        // Act
        ExcepcionNegocio error = capturar(() -> apartamento.cambiarEstadoPorPersonalDeServicio(FUERA_DE_SERVICIO));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.OPERACION_NO_PERMITIDA_PERSONAL_SERVICIO);
    }

    @Test
    @DisplayName("Un apartamento ocupado no puede declararse fuera de servicio")
    void ocupadoNoPasaAFueraDeServicio() {
        // Arrange
        Apartamento apartamento = apartamento("BH-201");
        apartamento.recibirGrupo();

        // Act
        ExcepcionNegocio error = capturar(apartamento::declararFueraDeServicio);

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.TRANSICION_ESTADO_OPERATIVO_INVALIDA);
    }

    @Test
    @DisplayName("Fuera de servicio no recibe grupos y vuelve pasando por preparación")
    void rehabilitacion() {
        // Arrange
        Apartamento apartamento = apartamento("BH-201");
        apartamento.declararFueraDeServicio();
        boolean podiaRecibirFueraDeServicio = apartamento.puedeRecibirGrupo();

        // Act
        apartamento.rehabilitar();

        // Assert
        assertThat(podiaRecibirFueraDeServicio).isFalse();
        assertThat(apartamento.getEstadoOperativo()).isEqualTo(PENDIENTE_PREPARACION);
    }

    @Test
    @DisplayName("Dos apartamentos con el mismo código son el mismo aunque cambien sus datos")
    void identidadPorCodigo() {
        // Arrange
        Apartamento original = apartamento("BH-201");
        Apartamento renombrado = apartamento("BH-201");
        renombrado.actualizarDatos("Cafetal renovado", "", 2, Set.of(), false);

        // Act
        boolean mismoApartamento = original.equals(renombrado);

        // Assert
        assertThat(mismoApartamento).isTrue();
        assertThat(original).isNotEqualTo(apartamento("BH-202"));
    }

    // --- Bloqueos ---

    @Test
    @DisplayName("No se puede bloquear noches que ya tienen reservas activas")
    void bloqueoSobreReservas() {
        // Arrange
        RangoFechas cruzaLaReserva = new RangoFechas(LocalDate.of(2026, 11, 11), LocalDate.of(2026, 11, 15));

        // Act
        ExcepcionNegocio error = capturar(
                () -> Bloqueo.registrar("BH-201", cruzaLaReserva, "Mantenimiento", List.of(NOCHES_RESERVADAS)));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.BLOQUEO_SOBRE_RESERVAS);
    }

    @Test
    @DisplayName("Se puede bloquear desde la noche de salida de una reserva")
    void bloqueoContiguoAReserva() {
        // Arrange
        RangoFechas desdeLaSalida = new RangoFechas(LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 15));

        // Act
        Bloqueo bloqueo = Bloqueo.registrar("BH-201", desdeLaSalida, "Mantenimiento", List.of(NOCHES_RESERVADAS));

        // Assert
        assertThat(bloqueo.motivo()).isEqualTo("Mantenimiento");
        assertThat(bloqueo.rango()).isEqualTo(desdeLaSalida);
    }

    @Test
    @DisplayName("Un bloqueo exige motivo")
    void bloqueoSinMotivo() {
        // Arrange
        RangoFechas rango = new RangoFechas(LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 15));

        // Act
        ExcepcionNegocio error = capturar(() -> Bloqueo.registrar("BH-201", rango, " ", List.of()));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.BLOQUEO_INVALIDO);
    }
}
