package co.edu.uniquindio.sga.infrastructure.persistencia.canal;

import co.edu.uniquindio.sga.domain.canal.ConflictoCanal;
import co.edu.uniquindio.sga.domain.canal.ConflictoCanalRepositorio;
import co.edu.uniquindio.sga.domain.comun.Pagina;
import co.edu.uniquindio.sga.domain.reserva.Estancia;
import co.edu.uniquindio.sga.domain.reserva.ReferenciaExterna;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Adaptador JPA del puerto {@link ConflictoCanalRepositorio}. */
@Repository
@Transactional
public class ConflictoCanalAdaptador implements ConflictoCanalRepositorio {

    public interface Jpa extends JpaRepository<ConflictoCanalEntity, String> {

        Page<ConflictoCanalEntity> findByRevisadoFalse(Pageable pagina);
    }

    private final Jpa jpa;

    public ConflictoCanalAdaptador(Jpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public void guardar(ConflictoCanal c) {
        ConflictoCanalEntity e = new ConflictoCanalEntity();
        e.setId(c.id());
        e.setCanal(c.referencia().canal());
        e.setIdExterno(c.referencia().idExterno());
        e.setCodigoApartamento(c.codigoApartamento());
        e.setEntrada(c.estancia().fechaEntrada());
        e.setSalida(c.estancia().fechaSalida());
        e.setTitular(c.titular());
        e.setMotivo(c.motivo());
        e.setRegistradoEn(c.registradoEn());
        e.setRevisado(!c.estaPendiente());
        if (c.revision() != null) {
            e.setRevisadoPor(c.revision().autor());
            e.setNotaRevision(c.revision().nota());
            e.setFechaRevision(c.revision().fecha());
        }
        jpa.save(e);
    }

    @Override
    public Optional<ConflictoCanal> buscar(String id) {
        return id == null ? Optional.empty() : jpa.findById(id).map(ConflictoCanalAdaptador::aDominio);
    }

    @Override
    public Pagina<ConflictoCanal> listar(boolean soloPendientes, int pagina) {
        Pageable orden = PageRequest.of(Math.max(pagina, 0), Pagina.TAMANO, Sort.by(Sort.Direction.DESC, "registradoEn"));
        Page<ConflictoCanalEntity> resultado = soloPendientes ? jpa.findByRevisadoFalse(orden) : jpa.findAll(orden);
        return new Pagina<>(resultado.map(ConflictoCanalAdaptador::aDominio).getContent(), resultado.getNumber(),
                resultado.getTotalElements());
    }

    private static ConflictoCanal aDominio(ConflictoCanalEntity e) {
        ConflictoCanal.Revision revision = e.isRevisado()
                ? new ConflictoCanal.Revision(e.getRevisadoPor(), e.getNotaRevision(), e.getFechaRevision())
                : null;
        return new ConflictoCanal(e.getId(), new ReferenciaExterna(e.getCanal(), e.getIdExterno()),
                e.getCodigoApartamento(), new Estancia(e.getEntrada(), e.getSalida()), e.getTitular(), e.getMotivo(),
                e.getRegistradoEn(), revision);
    }
}
