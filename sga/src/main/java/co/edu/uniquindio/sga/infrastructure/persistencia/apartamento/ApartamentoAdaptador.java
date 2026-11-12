package co.edu.uniquindio.sga.infrastructure.persistencia.apartamento;

import co.edu.uniquindio.sga.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga.domain.apartamento.ApartamentoRepositorio;
import co.edu.uniquindio.sga.domain.apartamento.ImagenApartamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Adaptador JPA del puerto {@link ApartamentoRepositorio}. */
@Repository
@Transactional
public class ApartamentoAdaptador implements ApartamentoRepositorio {

    public interface Jpa extends JpaRepository<ApartamentoEntity, String> {

        List<ApartamentoEntity> findByEliminadoFalse();

        /** Bloqueo pesimista: serializa las reservas concurrentes sobre el mismo apartamento. */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select a from ApartamentoEntity a where a.codigo = :codigo")
        Optional<ApartamentoEntity> buscarBloqueando(@Param("codigo") String codigo);
    }

    private final Jpa jpa;

    public ApartamentoAdaptador(Jpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public void guardar(Apartamento a) {
        ApartamentoEntity e = jpa.findById(a.getCodigo()).orElseGet(ApartamentoEntity::new);
        e.setCodigo(a.getCodigo());
        e.setNombre(a.getNombre());
        e.setDescripcion(a.getDescripcion());
        e.setDormitorios(a.getDormitorios());
        e.setCapacidad(a.getCapacidad());
        e.setAdmiteMascotas(a.admiteMascotas());
        e.setActivo(a.isActivo());
        e.setEliminado(a.isEliminado());
        e.setEstadoOperativo(a.getEstadoOperativo());
        e.getCaracteristicas().clear();
        e.getCaracteristicas().addAll(a.getCaracteristicas());
        e.getImagenes().clear();
        a.getImagenes().forEach(i -> e.getImagenes().add(new ApartamentoEntity.Imagen(i.url(), i.principal())));
        jpa.save(e);
    }

    @Override
    public Optional<Apartamento> buscar(String codigo) {
        return codigo == null ? Optional.empty() : jpa.findById(normalizar(codigo)).map(ApartamentoAdaptador::aDominio);
    }

    @Override
    public Optional<Apartamento> buscarParaActualizar(String codigo) {
        return codigo == null ? Optional.empty()
                : jpa.buscarBloqueando(normalizar(codigo)).map(ApartamentoAdaptador::aDominio);
    }

    @Override
    public boolean existe(String codigo) {
        return codigo != null && jpa.existsById(normalizar(codigo));
    }

    @Override
    public List<Apartamento> listarNoEliminados() {
        return jpa.findByEliminadoFalse().stream().map(ApartamentoAdaptador::aDominio).toList();
    }

    static String normalizar(String codigo) {
        return codigo.trim().toUpperCase();
    }

    private static Apartamento aDominio(ApartamentoEntity e) {
        return Apartamento.reconstituir(e.getCodigo(), e.getNombre(), e.getDescripcion(), e.getDormitorios(),
                e.getCapacidad(), e.getCaracteristicas(), e.isAdmiteMascotas(),
                e.getImagenes().stream().map(i -> new ImagenApartamento(i.getUrl(), i.isPrincipal())).toList(),
                e.isActivo(), e.isEliminado(), e.getEstadoOperativo());
    }
}
