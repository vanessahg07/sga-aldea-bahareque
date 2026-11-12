package co.edu.uniquindio.sga.infrastructure.persistencia.reserva;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.Dinero;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import co.edu.uniquindio.sga.domain.comun.Pagina;
import co.edu.uniquindio.sga.domain.folio.*;
import co.edu.uniquindio.sga.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.politica.PoliticaRepositorio;
import co.edu.uniquindio.sga.domain.reserva.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Adaptador JPA del puerto {@link ReservaRepositorio}: guarda la reserva con su cotización y su folio. */
@Repository
@Transactional
public class ReservaAdaptador implements ReservaRepositorio {

    private static final List<EstadoReserva> ACTIVAS = Arrays.stream(EstadoReserva.values())
            .filter(EstadoReserva::esActiva).toList();

    public interface Jpa extends JpaRepository<ReservaEntity, String>, JpaSpecificationExecutor<ReservaEntity> {

        List<ReservaEntity> findByCodigoApartamentoAndEstadoIn(String codigoApartamento, Collection<EstadoReserva> estados);

        @Query("select r from ReservaEntity r where r.estado in :estados and r.entrada < :hasta and r.salida > :desde "
                + "order by r.codigoApartamento, r.entrada")
        List<ReservaEntity> activasEnRango(@Param("estados") Collection<EstadoReserva> estados,
                                           @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

        Optional<ReservaEntity> findByCanalExternoAndIdExterno(String canalExterno, String idExterno);

        List<ReservaEntity> findByEstado(EstadoReserva estado);

        List<ReservaEntity> findByEntradaOrderByCodigoApartamento(LocalDate entrada);

        List<ReservaEntity> findBySalidaOrderByCodigoApartamento(LocalDate salida);

        boolean existsByTitularIdCuentaAndEstadoIn(String idCuenta, Collection<EstadoReserva> estados);

        @Query("select distinct r from ReservaEntity r left join r.pagos p where "
                + "(r.entrada <= :hasta and r.salida > :desde) "
                + "or (r.creadaEn >= :inicio and r.creadaEn < :fin) "
                + "or (p.fecha >= :inicio and p.fecha < :fin)")
        List<ReservaEntity> paraReportes(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
                                         @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
    }

    private final Jpa jpa;
    private final PoliticaRepositorio politicas;

    public ReservaAdaptador(Jpa jpa, PoliticaRepositorio politicas) {
        this.jpa = jpa;
        this.politicas = politicas;
    }

    // --- Escritura ---

    @Override
    public void guardar(Reserva r) {
        ReservaEntity e = jpa.findById(r.getId()).orElseGet(ReservaEntity::new);
        e.setId(r.getId());
        Titular t = r.getTitular();
        e.setTitularNombre(t.nombre());
        e.setTitularDocumento(t.documento());
        e.setTitularCorreo(t.correo());
        e.setTitularTelefono(t.telefono());
        e.setTitularFechaNacimiento(t.fechaNacimiento());
        e.setTitularIdCuenta(t.idCuenta());
        e.getAcompanantes().clear();
        r.getAcompanantes().forEach(o -> e.getAcompanantes().add(
                new ReservaEntity.Acompanante(o.nombre(), o.fechaNacimiento())));
        e.setCodigoApartamento(r.getCodigoApartamento());
        e.setEntrada(r.getEstancia().fechaEntrada());
        e.setSalida(r.getEstancia().fechaSalida());
        e.setHoraEstimadaLlegada(r.getHoraEstimadaLlegada().orElse(null));
        e.setCanal(r.getCanal());
        e.setCanalExterno(r.getReferenciaExterna().map(ReferenciaExterna::canal).orElse(null));
        e.setIdExterno(r.getReferenciaExterna().map(ReferenciaExterna::idExterno).orElse(null));
        e.setMascotas(r.getMascotas());
        e.setEstado(r.getEstado());
        e.setCreadaEn(r.getCreadaEn());
        e.setVersionPolitica(r.getPolitica().version());

        CotizacionEstancia c = r.getCotizacion();
        e.setSubtotalNoches(c.subtotalNoches().valor());
        e.setDescuento(c.descuento().valor());
        e.setValorAlojamiento(c.valorAlojamiento().valor());
        e.setValorMascotas(c.valorMascotas().valor());
        e.getDetalle().clear();
        c.detalle().forEach(n -> e.getDetalle().add(new ReservaEntity.DetalleNoche(n.fecha(), n.temporada(),
                n.tarifa().valor(), n.ocupantesFacturables(), n.subtotal().valor())));

        Folio f = r.getFolio();
        e.setFolioId(f.getId());
        e.setFolioCerrado(f.isCerrado());
        Optional<AutorizacionCierre> autorizacion = f.getAutorizacionCierre();
        e.setAutorizacionAutor(autorizacion.map(AutorizacionCierre::autor).orElse(null));
        e.setAutorizacionMotivo(autorizacion.map(AutorizacionCierre::motivo).orElse(null));
        e.setAutorizacionFecha(autorizacion.map(AutorizacionCierre::fecha).orElse(null));
        e.getCargos().clear();
        f.getCargos().forEach(g -> e.getCargos().add(new ReservaEntity.Cargo(g.id(), g.tipo(), g.valor().valor(),
                g.descripcion(), g.fecha(), g.idCargoRevertido())));
        e.getPagos().clear();
        f.getPagos().forEach(p -> e.getPagos().add(new ReservaEntity.Pago(p.id(), p.valor().valor(),
                p.medio().nombre(), p.fecha(), p.idPagoRevertido())));
        jpa.save(e);
    }

    // --- Consultas ---

    @Override
    public Optional<Reserva> buscar(String id) {
        return id == null ? Optional.empty() : jpa.findById(id).map(this::aDominio);
    }

    @Override
    public Optional<Reserva> buscarPorReferenciaExterna(ReferenciaExterna referencia) {
        return jpa.findByCanalExternoAndIdExterno(referencia.canal(), referencia.idExterno()).map(this::aDominio);
    }

    @Override
    public List<Reserva> activasDe(String codigoApartamento) {
        return aDominio(jpa.findByCodigoApartamentoAndEstadoIn(codigoApartamento.trim().toUpperCase(), ACTIVAS));
    }

    @Override
    public List<Reserva> activasEnRango(LocalDate desde, LocalDate hasta) {
        return aDominio(jpa.activasEnRango(ACTIVAS, desde, hasta));
    }

    @Override
    public Pagina<Reserva> listar(FiltroReservas filtro, int pagina) {
        Page<ReservaEntity> resultado = jpa.findAll(especificacion(filtro),
                PageRequest.of(Math.max(pagina, 0), Pagina.TAMANO, Sort.by(Sort.Direction.DESC, "creadaEn")));
        return new Pagina<>(aDominio(resultado.getContent()), resultado.getNumber(), resultado.getTotalElements());
    }

    @Override
    public List<Reserva> pendientes() {
        return aDominio(jpa.findByEstado(EstadoReserva.PENDIENTE));
    }

    @Override
    public List<Reserva> conEntradaEl(LocalDate fecha) {
        return aDominio(jpa.findByEntradaOrderByCodigoApartamento(fecha));
    }

    @Override
    public List<Reserva> conSalidaEl(LocalDate fecha) {
        return aDominio(jpa.findBySalidaOrderByCodigoApartamento(fecha));
    }

    @Override
    public boolean existenActivasDeCuenta(String idCuenta) {
        return jpa.existsByTitularIdCuentaAndEstadoIn(idCuenta, ACTIVAS);
    }

    @Override
    public List<Reserva> paraReportes(LocalDate desde, LocalDate hasta) {
        return aDominio(jpa.paraReportes(desde, hasta, desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay()));
    }

    private static Specification<ReservaEntity> especificacion(FiltroReservas f) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (f.desde() != null) condiciones.add(cb.greaterThan(raiz.get("salida"), f.desde()));
            if (f.hasta() != null) condiciones.add(cb.lessThan(raiz.get("entrada"), f.hasta()));
            if (f.estado() != null) condiciones.add(cb.equal(raiz.get("estado"), f.estado()));
            if (f.codigoApartamento() != null)
                condiciones.add(cb.equal(raiz.get("codigoApartamento"), f.codigoApartamento().trim().toUpperCase()));
            if (f.canal() != null) condiciones.add(cb.equal(raiz.get("canal"), f.canal()));
            if (f.idCuenta() != null) condiciones.add(cb.equal(raiz.get("titularIdCuenta"), f.idCuenta()));
            if (f.titular() != null && !f.titular().isBlank()) {
                String patron = "%" + f.titular().trim().toLowerCase() + "%";
                condiciones.add(cb.or(cb.like(cb.lower(raiz.get("titularNombre")), patron),
                        cb.like(cb.lower(raiz.get("titularDocumento")), patron)));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }

    // --- Mapeo ---

    private List<Reserva> aDominio(List<ReservaEntity> entidades) {
        Map<Integer, PoliticaCancelacion> cache = new HashMap<>();
        return entidades.stream().map(e -> aDominio(e, cache)).toList();
    }

    private Reserva aDominio(ReservaEntity e) {
        return aDominio(e, new HashMap<>());
    }

    private Reserva aDominio(ReservaEntity e, Map<Integer, PoliticaCancelacion> cache) {
        Titular titular = new Titular(e.getTitularNombre(), e.getTitularDocumento(), e.getTitularCorreo(),
                e.getTitularTelefono(), e.getTitularFechaNacimiento(), e.getTitularIdCuenta());
        ReferenciaExterna referencia = e.getIdExterno() == null ? null
                : new ReferenciaExterna(e.getCanalExterno(), e.getIdExterno());
        SolicitudReserva datos = new SolicitudReserva(titular,
                e.getAcompanantes().stream().map(a -> new Ocupante(a.getNombre(), a.getFechaNacimiento())).toList(),
                new Estancia(e.getEntrada(), e.getSalida()), e.getHoraEstimadaLlegada(), e.getCanal(), referencia,
                e.getMascotas());
        CotizacionEstancia cotizacion = new CotizacionEstancia(
                e.getDetalle().stream().map(n -> new co.edu.uniquindio.sga.domain.reserva.DetalleNoche(n.getFecha(),
                        n.getTemporada(), Dinero.de(n.getTarifa()), n.getOcupantesFacturables(),
                        Dinero.de(n.getSubtotal()))).toList(),
                Dinero.de(e.getSubtotalNoches()), Dinero.de(e.getDescuento()), Dinero.de(e.getValorAlojamiento()),
                Dinero.de(e.getValorMascotas()));
        PoliticaCancelacion politica = cache.computeIfAbsent(e.getVersionPolitica(), v -> politicas.porVersion(v)
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.POLITICA_NO_CONFIGURADA, "versión " + v)));
        AutorizacionCierre autorizacion = e.getAutorizacionAutor() == null ? null
                : new AutorizacionCierre(e.getAutorizacionAutor(), e.getAutorizacionMotivo(), e.getAutorizacionFecha());
        Folio folio = Folio.reconstituir(e.getFolioId(), e.getId(),
                e.getCargos().stream().map(g -> new Cargo(g.getIdCargo(), g.getTipo(), Dinero.de(g.getValor()),
                        g.getDescripcion(), g.getFecha(), g.getIdCargoRevertido())).toList(),
                e.getPagos().stream().map(p -> new Pago(p.getIdPago(), Dinero.de(p.getValor()),
                        new MedioPago(p.getMedio()), p.getFecha(), p.getIdPagoRevertido())).toList(),
                e.isFolioCerrado(), autorizacion);
        return Reserva.reconstituir(e.getId(), datos, e.getCodigoApartamento(), e.getEstado(), e.getCreadaEn(),
                cotizacion, politica, folio);
    }
}
