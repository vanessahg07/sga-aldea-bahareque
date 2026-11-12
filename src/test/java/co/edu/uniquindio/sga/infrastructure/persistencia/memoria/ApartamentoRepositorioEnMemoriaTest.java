package co.edu.uniquindio.sga.infrastructure.persistencia.memoria;

import co.edu.uniquindio.sga.domain.apartamento.Apartamento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static co.edu.uniquindio.sga.domain.Escenario.apartamento;
import static org.assertj.core.api.Assertions.assertThat;

class ApartamentoRepositorioEnMemoriaTest {

    private final ApartamentoRepositorioEnMemoria repositorio = new ApartamentoRepositorioEnMemoria();

    @Test
    @DisplayName("Un apartamento guardado se recupera por su código")
    void guardarYBuscar() {
        // Arrange
        Apartamento guadua = apartamento("BH-101");
        repositorio.guardar(guadua);

        // Act
        Optional<Apartamento> encontrado = repositorio.buscar("BH-101");

        // Assert
        assertThat(encontrado).contains(guadua);
        assertThat(repositorio.existe("BH-101")).isTrue();
    }

    @Test
    @DisplayName("Guardar dos veces el mismo código actualiza el apartamento en lugar de duplicarlo")
    void guardarActualiza() {
        // Arrange
        Apartamento guadua = apartamento("BH-101");
        repositorio.guardar(guadua);
        guadua.cambiarCapacidad(3);

        // Act
        repositorio.guardar(guadua);

        // Assert
        assertThat(repositorio.listarNoEliminados()).hasSize(1);
        assertThat(repositorio.buscar("BH-101")).map(Apartamento::getCapacidad).contains(3);
    }

    @Test
    @DisplayName("El listado ignora los apartamentos eliminados lógicamente")
    void listarIgnoraEliminados() {
        // Arrange
        Apartamento guadua = apartamento("BH-101");
        Apartamento yarumo = apartamento("BH-102");
        yarumo.eliminar(false);
        repositorio.guardar(guadua);
        repositorio.guardar(yarumo);

        // Act
        List<Apartamento> vigentes = repositorio.listarNoEliminados();

        // Assert
        assertThat(vigentes).containsExactly(guadua);
    }
}
