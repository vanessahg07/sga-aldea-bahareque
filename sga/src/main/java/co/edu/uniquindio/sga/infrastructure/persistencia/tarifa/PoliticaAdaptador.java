package co.edu.uniquindio.sga.infrastructure.persistencia.tarifa;

import co.edu.uniquindio.sga.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.politica.PoliticaRepositorio;
import co.edu.uniquindio.sga.domain.politica.TramoCancelacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Adaptador JPA del puerto {@link PoliticaRepositorio}. */
@Repository
@Transactional
public class PoliticaAdaptador implements PoliticaRepositorio {

    public interface Jpa extends JpaRepository<PoliticaEntity, Integer> {

        Optional<PoliticaEntity> findFirstByOrderByVersionDesc();

        List<PoliticaEntity> findAllByOrderByVersionAsc();
    }

    private final Jpa jpa;

    public PoliticaAdaptador(Jpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<PoliticaCancelacion> vigente() {
        return jpa.findFirstByOrderByVersionDesc().map(PoliticaAdaptador::aDominio);
    }

    @Override
    public Optional<PoliticaCancelacion> porVersion(int version) {
        return jpa.findById(version).map(PoliticaAdaptador::aDominio);
    }

    @Override
    public List<PoliticaCancelacion> todas() {
        return jpa.findAllByOrderByVersionAsc().stream().map(PoliticaAdaptador::aDominio).toList();
    }

    @Override
    public void guardar(PoliticaCancelacion politica) {
        PoliticaEntity e = new PoliticaEntity();
        e.setVersion(politica.version());
        e.setVigenteDesde(politica.vigenteDesde());
        e.setPorcentajeRetencionNoShow(politica.porcentajeRetencionNoShow());
        politica.tramos().forEach(t -> e.getTramos().add(
                new PoliticaEntity.Tramo(t.diasAntelacionMinimos(), t.porcentajeRetencion())));
        jpa.save(e);
    }

    private static PoliticaCancelacion aDominio(PoliticaEntity e) {
        return new PoliticaCancelacion(e.getVersion(), e.getVigenteDesde(),
                e.getTramos().stream().map(t -> new TramoCancelacion(t.getDiasAntelacionMinimos(),
                        t.getPorcentajeRetencion())).toList(),
                e.getPorcentajeRetencionNoShow());
    }
}
