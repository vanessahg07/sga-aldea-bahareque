package co.edu.uniquindio.sga.infrastructure.persistencia.usuario;

import co.edu.uniquindio.sga.domain.usuario.CodigoRecuperacion;
import co.edu.uniquindio.sga.domain.usuario.CodigoRecuperacionRepositorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Adaptador JPA del puerto {@link CodigoRecuperacionRepositorio}. */
@Repository
@Transactional
public class CodigoRecuperacionAdaptador implements CodigoRecuperacionRepositorio {

    public interface Jpa extends JpaRepository<CodigoRecuperacionEntity, String> {

        Optional<CodigoRecuperacionEntity> findFirstByCorreoOrderByExpiraEnDesc(String correo);
    }

    private final Jpa jpa;

    public CodigoRecuperacionAdaptador(Jpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public void guardar(CodigoRecuperacion codigo) {
        CodigoRecuperacionEntity e = new CodigoRecuperacionEntity();
        e.setId(codigo.getId());
        e.setCorreo(codigo.getCorreo());
        e.setCodigoCifrado(codigo.getCodigoCifrado());
        e.setExpiraEn(codigo.getExpiraEn());
        e.setUsado(codigo.isUsado());
        jpa.save(e);
    }

    @Override
    public Optional<CodigoRecuperacion> ultimoDe(String correo) {
        return jpa.findFirstByCorreoOrderByExpiraEnDesc(correo).map(e -> CodigoRecuperacion.reconstituir(
                e.getId(), e.getCorreo(), e.getCodigoCifrado(), e.getExpiraEn(), e.isUsado()));
    }
}
