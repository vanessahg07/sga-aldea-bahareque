package co.edu.uniquindio.sga.application.reserva;

import co.edu.uniquindio.sga.application.comun.ContextoVigente;
import co.edu.uniquindio.sga.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga.domain.apartamento.ApartamentoRepositorio;
import co.edu.uniquindio.sga.domain.apartamento.BloqueoRepositorio;
import co.edu.uniquindio.sga.domain.comun.*;
import co.edu.uniquindio.sga.domain.reserva.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

/** Búsqueda de apartamentos disponibles y cotización de estancias. */
@Service
@Transactional(readOnly = true)
public class DisponibilidadServicio {

    /**
     * Criterios de búsqueda. El grupo se describe con las fechas de nacimiento de sus
     * integrantes; si solo se conoce la cantidad, se asume que todos son facturables.
     */
    public record Criterios(LocalDate entrada, LocalDate salida, List<LocalDate> fechasNacimiento, Integer personas,
                            Integer capacidadMinima, BigDecimal precioMinimo, BigDecimal precioMaximo,
                            Set<String> caracteristicas, int mascotas) {
    }

    public record Resultado(Apartamento apartamento, CotizacionEstancia cotizacion) {
    }

    /** Cotización con la indicación de si el apartamento puede venderse y, si no, por qué. */
    public record Cotizacion(Apartamento apartamento, Estancia estancia, CotizacionEstancia cotizacion,
                             boolean disponible, CodigoError motivoNoDisponible) {
    }

    private final ApartamentoRepositorio apartamentos;
    private final ReservaRepositorio reservas;
    private final BloqueoRepositorio bloqueos;
    private final ContextoVigente contextoVigente;
    private final VerificadorDisponibilidad verificador;
    private final CotizadorEstancia cotizador;
    private final Reloj reloj;

    public DisponibilidadServicio(ApartamentoRepositorio apartamentos, ReservaRepositorio reservas,
                                  BloqueoRepositorio bloqueos, ContextoVigente contextoVigente,
                                  VerificadorDisponibilidad verificador, CotizadorEstancia cotizador, Reloj reloj) {
        this.apartamentos = apartamentos;
        this.reservas = reservas;
        this.bloqueos = bloqueos;
        this.contextoVigente = contextoVigente;
        this.verificador = verificador;
        this.cotizador = cotizador;
        this.reloj = reloj;
    }

    public Pagina<Resultado> buscar(Criterios criterios, int pagina) {
        return Pagina.de(disponibles(criterios), pagina);
    }

    /** Todos los apartamentos que pueden venderse para los criterios, del más económico al más costoso. */
    public List<Resultado> disponibles(Criterios criterios) {
        Estancia estancia = new Estancia(criterios.entrada(), criterios.salida());
        List<Ocupante> grupo = grupo(criterios.fechasNacimiento(), criterios.personas(), estancia.fechaEntrada());
        ContextoReserva contexto = contextoVigente.contextoReserva();
        LocalDate hoy = reloj.hoy();

        return apartamentos.listarNoEliminados().stream()
                .filter(Apartamento::estaALaVenta)
                .filter(a -> criterios.capacidadMinima() == null || a.getCapacidad() >= criterios.capacidadMinima())
                .filter(a -> criterios.caracteristicas() == null
                        || a.getCaracteristicas().containsAll(criterios.caracteristicas()))
                .filter(a -> puedeVenderse(a, estancia, grupo.size(), criterios.mascotas(), contexto, hoy).isEmpty())
                .map(a -> new Resultado(a, cotizador.cotizar(a.getCodigo(), estancia, grupo, criterios.mascotas(), contexto)))
                .filter(r -> dentroDelPrecio(r.cotizacion().valorTotal(), criterios))
                .sorted(Comparator.comparing((Resultado r) -> r.cotizacion().valorTotal())
                        .thenComparing(r -> r.apartamento().getCodigo()))
                .toList();
    }

    public Cotizacion cotizar(String codigoApartamento, LocalDate entrada, LocalDate salida,
                              List<LocalDate> fechasNacimiento, Integer personas, int mascotas) {
        Apartamento apartamento = apartamentos.buscar(codigoApartamento)
                .filter(a -> !a.isEliminado())
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.APARTAMENTO_NO_ENCONTRADO, codigoApartamento));
        Estancia estancia = new Estancia(entrada, salida);
        List<Ocupante> grupo = grupo(fechasNacimiento, personas, entrada);
        ContextoReserva contexto = contextoVigente.contextoReserva();
        Optional<CodigoError> motivo = puedeVenderse(apartamento, estancia, grupo.size(), mascotas, contexto, reloj.hoy());
        CotizacionEstancia cotizacion = cotizador.cotizar(apartamento.getCodigo(), estancia, grupo, mascotas, contexto);
        return new Cotizacion(apartamento, estancia, cotizacion, motivo.isEmpty(), motivo.orElse(null));
    }

    private Optional<CodigoError> puedeVenderse(Apartamento apartamento, Estancia estancia, int ocupantes, int mascotas,
                                                ContextoReserva contexto, LocalDate hoy) {
        try {
            OcupacionApartamento ocupacion = new OcupacionApartamento(apartamento,
                    reservas.activasDe(apartamento.getCodigo()), bloqueos.vigentesDe(apartamento.getCodigo()));
            verificador.verificar(ocupacion, estancia, ocupantes, mascotas, contexto, hoy, null);
            return Optional.empty();
        } catch (ExcepcionNegocio e) {
            return Optional.of(e.getCodigo());
        }
    }

    private static boolean dentroDelPrecio(Dinero valor, Criterios criterios) {
        return (criterios.precioMinimo() == null || !valor.esMenorQue(Dinero.de(criterios.precioMinimo())))
                && (criterios.precioMaximo() == null || !Dinero.de(criterios.precioMaximo()).esMenorQue(valor));
    }

    /** Construye el grupo a partir de las fechas de nacimiento o, en su defecto, de la cantidad de adultos. */
    static List<Ocupante> grupo(List<LocalDate> fechasNacimiento, Integer personas, LocalDate entrada) {
        if (fechasNacimiento != null && !fechasNacimiento.isEmpty()) {
            return IntStream.range(0, fechasNacimiento.size())
                    .mapToObj(i -> new Ocupante("Ocupante " + (i + 1), fechasNacimiento.get(i)))
                    .toList();
        }
        int cantidad = personas == null ? 1 : personas;
        ExcepcionNegocio.exigir(cantidad >= 1, CodigoError.OCUPANTE_INVALIDO, "cantidad de personas");
        LocalDate adulto = entrada.minusYears(30);
        return IntStream.range(0, cantidad).mapToObj(i -> new Ocupante("Ocupante " + (i + 1), adulto)).toList();
    }
}
