package co.edu.uniquindio.sga.infrastructure.persistencia.canal;

import co.edu.uniquindio.sga.domain.canal.CanalVenta;
import co.edu.uniquindio.sga.domain.canal.CanalVentaRepositorio;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Adaptador JPA del puerto {@link CanalVentaRepositorio}. */
@Repository
@Transactional
public class CanalVentaAdaptador implements CanalVentaRepositorio {

    public interface Jpa extends JpaRepository<CanalVentaEntity, String> {
    }

    private final Jpa jpa;

    public CanalVentaAdaptador(Jpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public void guardar(CanalVenta canal) {
        CanalVentaEntity e = new CanalVentaEntity();
        e.setCodigo(canal.getCodigo());
        e.setNombre(canal.getNombre());
        e.setTipo(canal.getTipo());
        e.setCredencialCifrada(canal.getCredencialCifrada());
        e.setActivo(canal.isActivo());
        jpa.save(e);
    }

    @Override
    public Optional<CanalVenta> buscar(String codigo) {
        return codigo == null ? Optional.empty() : jpa.findById(codigo).map(CanalVentaAdaptador::aDominio);
    }

    @Override
    public boolean existe(String codigo) {
        return jpa.existsById(codigo);
    }

    @Override
    public List<CanalVenta> listar() {
        return jpa.findAll(Sort.by("nombre")).stream().map(CanalVentaAdaptador::aDominio).toList();
    }

    private static CanalVenta aDominio(CanalVentaEntity e) {
        return CanalVenta.reconstituir(e.getCodigo(), e.getNombre(), e.getTipo(), e.getCredencialCifrada(), e.isActivo());
    }
}
