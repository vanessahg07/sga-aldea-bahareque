package co.edu.uniquindio.sga.application.tarifa;

import co.edu.uniquindio.sga.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga.domain.apartamento.ApartamentoRepositorio;
import co.edu.uniquindio.sga.domain.comun.*;
import co.edu.uniquindio.sga.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.politica.PoliticaRepositorio;
import co.edu.uniquindio.sga.domain.politica.TramoCancelacion;
import co.edu.uniquindio.sga.domain.tarifa.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Casos de uso de temporadas, tarifas y política de cancelación. */
@Service
@Transactional
public class TarifaServicio {

    private final TemporadaRepositorio temporadas;
    private final TarifaRepositorio tarifas;
    private final PoliticaRepositorio politicas;
    private final ApartamentoRepositorio apartamentos;
    private final Reloj reloj;

    public TarifaServicio(TemporadaRepositorio temporadas, TarifaRepositorio tarifas, PoliticaRepositorio politicas,
                          ApartamentoRepositorio apartamentos, Reloj reloj) {
        this.temporadas = temporadas;
        this.tarifas = tarifas;
        this.politicas = politicas;
        this.apartamentos = apartamentos;
        this.reloj = reloj;
    }

    // --- Temporadas ---

    @Transactional(readOnly = true)
    public List<Temporada> temporadas() {
        return temporadas.calendario().temporadas().stream()
                .sorted(Comparator.comparing(Temporada::esBase).reversed().thenComparing(Temporada::nombre))
                .toList();
    }

    /** Agrega una temporada. El calendario resultante debe seguir sin solapamientos. */
    public Temporada crearTemporada(String nombre, List<RangoFechas> rangos, int estanciaMinimaNoches) {
        List<Temporada> actuales = temporadas.calendario().temporadas();
        ExcepcionNegocio.exigir(actuales.stream().noneMatch(t -> t.nombre().equalsIgnoreCase(nombre)),
                CodigoError.TEMPORADA_YA_EXISTE, nombre);
        Temporada nueva = new Temporada(nombre.trim().toUpperCase(), false, rangos, estanciaMinimaNoches);
        List<Temporada> todas = new ArrayList<>(actuales);
        todas.add(nueva);
        temporadas.guardar(new CalendarioTemporadas(todas));
        return nueva;
    }

    /** Cambia los rangos y la estancia mínima. En la temporada base solo cambia la estancia mínima. */
    public Temporada actualizarTemporada(String nombre, List<RangoFechas> rangos, int estanciaMinimaNoches) {
        List<Temporada> todas = new ArrayList<>(temporadas.calendario().temporadas());
        Temporada actual = buscarTemporada(todas, nombre);
        Temporada nueva = new Temporada(actual.nombre(), actual.esBase(), actual.esBase() ? List.of() : rangos,
                estanciaMinimaNoches);
        todas.set(todas.indexOf(actual), nueva);
        temporadas.guardar(new CalendarioTemporadas(todas));
        return nueva;
    }

    /** Retira una temporada (eliminación lógica). La temporada base no puede retirarse. */
    public void eliminarTemporada(String nombre) {
        List<Temporada> todas = new ArrayList<>(temporadas.calendario().temporadas());
        Temporada actual = buscarTemporada(todas, nombre);
        todas.remove(actual);
        temporadas.guardar(new CalendarioTemporadas(todas));
    }

    private static Temporada buscarTemporada(List<Temporada> todas, String nombre) {
        return todas.stream().filter(t -> t.nombre().equalsIgnoreCase(nombre)).findFirst()
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.TEMPORADA_NO_ENCONTRADA, nombre));
    }

    // --- Tarifas ---

    /** Registra una nueva tarifa vigente desde ahora; las anteriores quedan en el histórico. */
    public Tarifa registrarTarifa(String codigoApartamento, String nombreTemporada, BigDecimal valor) {
        ExcepcionNegocio.exigir(apartamentos.existe(codigoApartamento), CodigoError.APARTAMENTO_NO_ENCONTRADO,
                codigoApartamento);
        Temporada temporada = buscarTemporada(temporadas.calendario().temporadas(), nombreTemporada);
        Tarifa tarifa = new Tarifa(codigoApartamento.toUpperCase(), temporada.nombre(), Dinero.de(valor), reloj.ahora());
        tarifas.registrar(tarifa);
        return tarifa;
    }

    @Transactional(readOnly = true)
    public List<Tarifa> historico(String codigoApartamento) {
        return tarifas.tabla().historico(codigoApartamento.toUpperCase());
    }

    /** Tarifas vigentes de un apartamento, una por temporada. */
    @Transactional(readOnly = true)
    public List<Tarifa> vigentes(String codigoApartamento) {
        TablaTarifas tabla = tarifas.tabla();
        return temporadas().stream()
                .flatMap(t -> tabla.tarifaVigente(codigoApartamento.toUpperCase(), t.nombre()).stream())
                .toList();
    }

    /** Apartamentos activos a los que les falta tarifa en alguna temporada: no pueden venderse. */
    @Transactional(readOnly = true)
    public List<String> apartamentosConTarifasIncompletas() {
        CalendarioTemporadas calendario = temporadas.calendario();
        TablaTarifas tabla = tarifas.tabla();
        return apartamentos.listarNoEliminados().stream()
                .filter(Apartamento::estaALaVenta)
                .map(Apartamento::getCodigo)
                .filter(codigo -> !tabla.tieneTarifasCompletas(codigo, calendario))
                .sorted()
                .toList();
    }

    // --- Política de cancelación ---

    @Transactional(readOnly = true)
    public PoliticaCancelacion politicaVigente() {
        return politicas.vigente().orElseThrow(() -> new ExcepcionNegocio(CodigoError.POLITICA_NO_CONFIGURADA));
    }

    @Transactional(readOnly = true)
    public List<PoliticaCancelacion> versionesPolitica() {
        return politicas.todas();
    }

    /** Publica una nueva versión. Las reservas existentes conservan la versión con la que se crearon. */
    public PoliticaCancelacion publicarPolitica(List<TramoCancelacion> tramos, BigDecimal porcentajeRetencionNoShow) {
        PoliticaCancelacion nueva = politicas.vigente()
                .map(p -> p.nuevaVersion(reloj.ahora(), tramos, porcentajeRetencionNoShow))
                .orElseGet(() -> new PoliticaCancelacion(1, reloj.ahora(), tramos, porcentajeRetencionNoShow));
        politicas.guardar(nueva);
        return nueva;
    }
}
