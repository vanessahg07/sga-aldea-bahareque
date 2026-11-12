package co.edu.uniquindio.sga.infrastructure.persistencia.tarifa;

import co.edu.uniquindio.sga.domain.comun.RangoFechas;
import co.edu.uniquindio.sga.domain.tarifa.CalendarioTemporadas;
import co.edu.uniquindio.sga.domain.tarifa.Temporada;
import co.edu.uniquindio.sga.domain.tarifa.TemporadaRepositorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Adaptador JPA del puerto {@link TemporadaRepositorio}. */
@Repository
@Transactional
public class TemporadaAdaptador implements TemporadaRepositorio {

    public interface Jpa extends JpaRepository<TemporadaEntity, String> {

        List<TemporadaEntity> findByActivaTrue();
    }

    private final Jpa jpa;

    public TemporadaAdaptador(Jpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public CalendarioTemporadas calendario() {
        return new CalendarioTemporadas(jpa.findByActivaTrue().stream()
                .map(e -> new Temporada(e.getNombre(), e.isEsBase(),
                        e.getRangos().stream().map(r -> new RangoFechas(r.getInicio(), r.getFin())).toList(),
                        e.getEstanciaMinimaNoches()))
                .toList());
    }

    @Override
    public void guardar(CalendarioTemporadas calendario) {
        Set<String> vigentes = calendario.temporadas().stream().map(Temporada::nombre).collect(Collectors.toSet());
        jpa.findByActivaTrue().stream()
                .filter(e -> !vigentes.contains(e.getNombre()))
                .forEach(e -> {
                    e.setActiva(false);
                    jpa.save(e);
                });
        for (Temporada temporada : calendario.temporadas()) {
            TemporadaEntity e = jpa.findById(temporada.nombre()).orElseGet(TemporadaEntity::new);
            e.setNombre(temporada.nombre());
            e.setEsBase(temporada.esBase());
            e.setEstanciaMinimaNoches(temporada.estanciaMinimaNoches());
            e.setActiva(true);
            e.getRangos().clear();
            temporada.rangos().forEach(r -> e.getRangos().add(new TemporadaEntity.Rango(r.inicio(), r.fin())));
            jpa.save(e);
        }
    }
}
