package co.edu.uniquindio.sga.infrastructure.persistencia.apartamento;

import co.edu.uniquindio.sga.domain.apartamento.Novedad;
import co.edu.uniquindio.sga.domain.apartamento.NovedadRepositorio;
import co.edu.uniquindio.sga.domain.comun.Pagina;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Adaptador JPA del puerto {@link NovedadRepositorio}. */
@Repository
@Transactional
public class NovedadAdaptador implements NovedadRepositorio {

    public interface Jpa extends JpaRepository<NovedadEntity, String> {

        Page<NovedadEntity> findByCodigoApartamentoOrderByFechaDesc(String codigoApartamento, Pageable pagina);
    }

    private final Jpa jpa;

    public NovedadAdaptador(Jpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public void guardar(Novedad novedad) {
        NovedadEntity e = new NovedadEntity();
        e.setId(novedad.id());
        e.setCodigoApartamento(novedad.codigoApartamento());
        e.setFecha(novedad.fecha());
        e.setAutor(novedad.autor());
        e.setDescripcion(novedad.descripcion());
        e.setGravedad(novedad.gravedad());
        jpa.save(e);
    }

    @Override
    public Pagina<Novedad> deApartamento(String codigoApartamento, int pagina) {
        Page<NovedadEntity> resultado = jpa.findByCodigoApartamentoOrderByFechaDesc(
                ApartamentoAdaptador.normalizar(codigoApartamento), PageRequest.of(Math.max(pagina, 0), Pagina.TAMANO));
        return new Pagina<>(resultado.map(e -> new Novedad(e.getId(), e.getCodigoApartamento(), e.getFecha(),
                e.getAutor(), e.getDescripcion(), e.getGravedad())).getContent(), resultado.getNumber(),
                resultado.getTotalElements());
    }
}
