package co.edu.uniquindio.sga.application.reporte;

import co.edu.uniquindio.sga.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga.domain.apartamento.ApartamentoRepositorio;
import co.edu.uniquindio.sga.domain.reporte.CalculadoraReportes;
import co.edu.uniquindio.sga.domain.reporte.Periodo;
import co.edu.uniquindio.sga.domain.reporte.Reportes;
import co.edu.uniquindio.sga.domain.reserva.Reserva;
import co.edu.uniquindio.sga.domain.reserva.ReservaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Reportes obligatorios (7.9) e indicadores propios (L-19), todos filtrados por periodo. */
@Service
@Transactional(readOnly = true)
public class ReporteServicio {

    private final ReservaRepositorio reservas;
    private final ApartamentoRepositorio apartamentos;
    private final CalculadoraReportes calculadora;

    public ReporteServicio(ReservaRepositorio reservas, ApartamentoRepositorio apartamentos,
                           CalculadoraReportes calculadora) {
        this.reservas = reservas;
        this.apartamentos = apartamentos;
        this.calculadora = calculadora;
    }

    public Reportes.Ocupacion ocupacion(LocalDate desde, LocalDate hasta) {
        Periodo periodo = new Periodo(desde, hasta);
        return calculadora.ocupacion(periodo, activos(), reservasDe(periodo));
    }

    public Reportes.Ingresos ingresos(LocalDate desde, LocalDate hasta) {
        Periodo periodo = new Periodo(desde, hasta);
        return calculadora.ingresos(periodo, reservasDe(periodo));
    }

    public Reportes.ReservasPorCanal porCanal(LocalDate desde, LocalDate hasta) {
        Periodo periodo = new Periodo(desde, hasta);
        return calculadora.porCanal(periodo, reservasDe(periodo));
    }

    public Reportes.Indicadores indicadores(LocalDate desde, LocalDate hasta) {
        Periodo periodo = new Periodo(desde, hasta);
        return calculadora.indicadores(periodo, activos().size(), reservasDe(periodo));
    }

    /**
     * Apartamentos a la venta hoy. El SGA no guarda el histórico de activaciones, por lo que
     * la base de noches disponibles usa el inventario vigente.
     */
    private List<String> activos() {
        return apartamentos.listarNoEliminados().stream()
                .filter(Apartamento::estaALaVenta)
                .map(Apartamento::getCodigo)
                .toList();
    }

    private List<Reserva> reservasDe(Periodo periodo) {
        return reservas.paraReportes(periodo.desde(), periodo.hasta());
    }
}
