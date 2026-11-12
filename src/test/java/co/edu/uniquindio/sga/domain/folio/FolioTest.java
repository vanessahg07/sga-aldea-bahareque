package co.edu.uniquindio.sga.domain.folio;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.Dinero;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static co.edu.uniquindio.sga.domain.Escenario.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * En todas las pruebas el folio parte abierto con un cargo de alojamiento de 340.000
 * (preparado en {@link #abrirFolio()}, parte común del paso Arrange).
 */
class FolioTest {

    private Folio folio;
    private Cargo alojamiento;

    @BeforeEach
    void abrirFolio() {
        folio = Folio.abrir("reserva-1");
        alojamiento = folio.agregarCargo(TipoCargo.ALOJAMIENTO, Dinero.de(340_000), "Alojamiento 2 noches", AHORA);
    }

    @Test
    @DisplayName("RN-15: el saldo es cargos menos pagos, admitiendo varios medios")
    void saldoConPagosParciales() {
        // Arrange
        folio.registrarPago(Dinero.de(100_000), TRANSFERENCIA, AHORA);
        folio.registrarPago(Dinero.de(40_000), EFECTIVO, AHORA.plusDays(1));

        // Act
        Dinero saldo = folio.saldo();

        // Assert
        assertThat(saldo).isEqualTo(Dinero.de(200_000));
        assertThat(folio.getPagos()).extracting(Pago::medio).containsExactly(TRANSFERENCIA, EFECTIVO);
    }

    @Test
    @DisplayName("RN-15: pagar exactamente los cargos deja el saldo en cero")
    void saldoCero() {
        // Arrange
        folio.registrarPago(Dinero.de(340_000), EFECTIVO, AHORA);

        // Act
        Dinero saldo = folio.saldo();

        // Assert
        assertThat(saldo).isEqualTo(Dinero.CERO);
    }

    @Test
    @DisplayName("RN-15: pagar de más deja un saldo negativo (saldo a favor)")
    void saldoNegativo() {
        // Arrange
        folio.registrarPago(Dinero.de(350_000), EFECTIVO, AHORA);

        // Act
        Dinero saldo = folio.saldo();

        // Assert
        assertThat(saldo).isEqualTo(Dinero.de(-10_000));
    }

    @Test
    @DisplayName("RN-15 violada: pago sin medio")
    void pagoSinMedio() {
        // Arrange
        MedioPago sinMedio = null;

        // Act
        ExcepcionNegocio error = capturar(() -> folio.registrarPago(Dinero.de(1_000), sinMedio, AHORA));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.PAGO_INVALIDO);
    }

    @Test
    @DisplayName("RN-15 violada: pago sin fecha")
    void pagoSinFecha() {
        // Arrange
        Dinero valor = Dinero.de(1_000);

        // Act
        ExcepcionNegocio error = capturar(() -> folio.registrarPago(valor, EFECTIVO, null));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.PAGO_INVALIDO);
    }

    @Test
    @DisplayName("RN-15 violada: pago de valor cero")
    void pagoEnCero() {
        // Arrange
        Dinero cero = Dinero.CERO;

        // Act
        ExcepcionNegocio error = capturar(() -> folio.registrarPago(cero, EFECTIVO, AHORA));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.PAGO_INVALIDO);
    }

    @Test
    @DisplayName("RN-16: los movimientos no se pueden eliminar desde fuera del folio")
    void movimientosInmutables() {
        // Arrange
        List<Cargo> cargos = folio.getCargos();

        // Act
        Throwable error = catchThrowable(cargos::clear);

        // Assert
        assertThat(error).isInstanceOf(UnsupportedOperationException.class);
        assertThat(folio.getCargos()).hasSize(1);
    }

    @Test
    @DisplayName("RN-16: la corrección de un cargo es un cargo inverso que lo referencia")
    void reversoDeCargo() {
        // Arrange
        String idCargo = alojamiento.id();

        // Act
        Cargo reverso = folio.revertirCargo(idCargo, "Error de digitación", AHORA);

        // Assert
        assertThat(reverso.valor()).isEqualTo(Dinero.de(-340_000));
        assertThat(reverso.idCargoRevertido()).isEqualTo(idCargo);
        assertThat(folio.getCargos()).hasSize(2);
        assertThat(folio.saldo()).isEqualTo(Dinero.CERO);
    }

    @Test
    @DisplayName("RN-16 violada: un cargo no se revierte dos veces")
    void cargoRevertidoDosVeces() {
        // Arrange
        folio.revertirCargo(alojamiento.id(), "Error", AHORA);

        // Act
        ExcepcionNegocio error = capturar(() -> folio.revertirCargo(alojamiento.id(), "Otra vez", AHORA));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.MOVIMIENTO_YA_REVERTIDO);
    }

    @Test
    @DisplayName("RN-16: la corrección de un pago es un pago inverso")
    void reversoDePago() {
        // Arrange
        Pago pago = folio.registrarPago(Dinero.de(50_000), EFECTIVO, AHORA);

        // Act
        Pago reverso = folio.revertirPago(pago.id(), AHORA);

        // Assert
        assertThat(reverso.valor()).isEqualTo(Dinero.de(-50_000));
        assertThat(folio.saldo()).isEqualTo(Dinero.de(340_000));
    }

    @Test
    @DisplayName("RN-16 violada: un pago no se revierte dos veces")
    void pagoRevertidoDosVeces() {
        // Arrange
        Pago pago = folio.registrarPago(Dinero.de(50_000), EFECTIVO, AHORA);
        folio.revertirPago(pago.id(), AHORA);

        // Act
        ExcepcionNegocio error = capturar(() -> folio.revertirPago(pago.id(), AHORA));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.MOVIMIENTO_YA_REVERTIDO);
    }

    @Test
    @DisplayName("RN-17 violada: no se cierra con saldo sin autorización")
    void cierreConSaldoSinAutorizacion() {
        // Arrange: el folio tiene saldo pendiente de 340.000

        // Act
        ExcepcionNegocio error = capturar(folio::cerrar);

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.FOLIO_CON_SALDO);
        assertThat(folio.isCerrado()).isFalse();
    }

    @Test
    @DisplayName("RN-17 cumplida: con saldo cero se cierra")
    void cierreConSaldoCero() {
        // Arrange
        folio.registrarPago(Dinero.de(340_000), EFECTIVO, AHORA);

        // Act
        folio.cerrar();

        // Assert
        assertThat(folio.isCerrado()).isTrue();
    }

    @Test
    @DisplayName("RN-17 cumplida: con autorización explícita se cierra y queda registrada")
    void cierreAutorizado() {
        // Arrange
        AutorizacionCierre autorizacion = new AutorizacionCierre("admin@aldea.co", "Cortesía por daño en la ducha", AHORA);

        // Act
        folio.cerrarConAutorizacion(autorizacion);

        // Assert
        assertThat(folio.isCerrado()).isTrue();
        assertThat(folio.getAutorizacionCierre()).contains(autorizacion);
    }

    @Test
    @DisplayName("La autorización exige autor y motivo")
    void autorizacionSinMotivo() {
        // Arrange
        String motivoVacio = " ";

        // Act
        ExcepcionNegocio error = capturar(() -> new AutorizacionCierre("admin", motivoVacio, AHORA));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.AUTORIZACION_INVALIDA);
    }

    @Test
    @DisplayName("Un folio cerrado no admite pagos")
    void folioCerradoNoAdmitePagos() {
        // Arrange
        folio.registrarPago(Dinero.de(340_000), EFECTIVO, AHORA);
        folio.cerrar();

        // Act
        ExcepcionNegocio error = capturar(() -> folio.registrarPago(Dinero.de(1_000), EFECTIVO, AHORA));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.FOLIO_CERRADO);
    }

    @Test
    @DisplayName("Un folio cerrado no admite cargos")
    void folioCerradoNoAdmiteCargos() {
        // Arrange
        folio.registrarPago(Dinero.de(340_000), EFECTIVO, AHORA);
        folio.cerrar();

        // Act
        ExcepcionNegocio error = capturar(
                () -> folio.agregarCargo(TipoCargo.SERVICIO_ADICIONAL, Dinero.de(25_000), "Desayuno", AHORA));

        // Assert
        assertThat(error.getCodigo()).isEqualTo(CodigoError.FOLIO_CERRADO);
    }

    @Test
    @DisplayName("Dos folios con el mismo identificador son el mismo folio")
    void identidadPorId() {
        // Arrange
        Folio reconstruido = Folio.reconstituir(folio.getId(), "reserva-1", List.of(), List.of(), false, null);

        // Act
        boolean mismoFolio = folio.equals(reconstruido);

        // Assert
        assertThat(mismoFolio).isTrue();
        assertThat(folio).isNotEqualTo(Folio.abrir("reserva-1"));
    }
}
