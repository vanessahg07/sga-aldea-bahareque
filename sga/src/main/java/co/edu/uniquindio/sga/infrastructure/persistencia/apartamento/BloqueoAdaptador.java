package co.edu.uniquindio.sga.infrastructure.persistencia.apartamento;

import co.edu.uniquindio.sga.domain.apartamento.Bloqueo;
import co.edu.uniquindio.sga.domain.apartamento.BloqueoRepositorio;
import co.edu.uniquindio.sga.domain.comun.RangoFechas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Adaptador JPA del puerto {@link BloqueoRepositorio}. */
@Repository
@Transactional
public class BloqueoAdaptador implements BloqueoRepositorio {

    public interface Jpa extends JpaRepository<BloqueoEntity, String> {

        List<BloqueoEntity> findByCodigoApartamentoAndVigenteTrueOrderByInicio(String codigoApartamento);

        Optional<BloqueoEntity> findByIdAndVigenteTrue(String id);
    }

    private final Jpa jpa;

    public BloqueoAdaptador(Jpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public void guardar(Bloqueo bloqueo) {
        BloqueoEntity e = new BloqueoEntity();
        e.setId(bloqueo.id());
        e.setCodigoApartamento(bloqueo.codigoApartamento());
        e.setInicio(bloqueo.rango().inicio());
        e.setFin(bloqueo.rango().fin());
        e.setMotivo(bloqueo.motivo());
        e.setVigente(true);
        jpa.save(e);
    }

    @Override
    public Optional<Bloqueo> buscarVigente(String id) {
        return jpa.findByIdAndVigenteTrue(id).map(BloqueoAdaptador::aDominio);
    }

    @Override
    public List<Bloqueo> vigentesDe(String codigoApartamento) {
        return jpa.findByCodigoApartamentoAndVigenteTrueOrderByInicio(ApartamentoAdaptador.normalizar(codigoApartamento))
                .stream().map(BloqueoAdaptador::aDominio).toList();
    }

    @Override
    public void levantar(String id) {
        jpa.findById(id).ifPresent(e -> {
            e.setVigente(false);
            jpa.save(e);
        });
    }

    private static Bloqueo aDominio(BloqueoEntity e) {
        return new Bloqueo(e.getId(), e.getCodigoApartamento(), new RangoFechas(e.getInicio(), e.getFin()), e.getMotivo());
    }
}
