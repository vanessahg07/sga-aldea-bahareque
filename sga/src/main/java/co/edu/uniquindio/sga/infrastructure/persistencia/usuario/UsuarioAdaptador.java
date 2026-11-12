package co.edu.uniquindio.sga.infrastructure.persistencia.usuario;

import co.edu.uniquindio.sga.domain.comun.Pagina;
import co.edu.uniquindio.sga.domain.usuario.Usuario;
import co.edu.uniquindio.sga.domain.usuario.UsuarioRepositorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Adaptador JPA del puerto {@link UsuarioRepositorio}. */
@Repository
@Transactional
public class UsuarioAdaptador implements UsuarioRepositorio {

    public interface Jpa extends JpaRepository<UsuarioEntity, String> {

        Optional<UsuarioEntity> findByCorreoAndEliminadoFalse(String correo);

        boolean existsByCorreo(String correo);

        Page<UsuarioEntity> findByEliminadoFalse(Pageable pagina);
    }

    private final Jpa jpa;

    public UsuarioAdaptador(Jpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public void guardar(Usuario u) {
        UsuarioEntity e = jpa.findById(u.getId()).orElseGet(UsuarioEntity::new);
        e.setId(u.getId());
        e.setNombre(u.getNombre());
        e.setCorreo(u.getCorreo());
        e.setContrasenaCifrada(u.getContrasenaCifrada());
        e.setTelefono(u.getTelefono());
        e.setDocumento(u.getDocumento());
        e.setFechaNacimiento(u.getFechaNacimiento());
        e.setFotoUrl(u.getFotoUrl());
        e.setRol(u.getRol());
        e.setEliminado(u.isEliminado());
        jpa.save(e);
    }

    @Override
    public Optional<Usuario> buscar(String id) {
        return id == null ? Optional.empty() : jpa.findById(id).map(UsuarioAdaptador::aDominio);
    }

    @Override
    public Optional<Usuario> buscarPorCorreo(String correo) {
        return correo == null ? Optional.empty()
                : jpa.findByCorreoAndEliminadoFalse(Usuario.normalizarCorreo(correo)).map(UsuarioAdaptador::aDominio);
    }

    /** Incluye usuarios eliminados: el correo sigue siendo único. */
    @Override
    public boolean existeCorreo(String correo) {
        return correo != null && jpa.existsByCorreo(Usuario.normalizarCorreo(correo));
    }

    @Override
    public Pagina<Usuario> listar(int pagina) {
        Page<UsuarioEntity> resultado = jpa.findByEliminadoFalse(
                PageRequest.of(Math.max(pagina, 0), Pagina.TAMANO, Sort.by("nombre")));
        return new Pagina<>(resultado.map(UsuarioAdaptador::aDominio).getContent(), resultado.getNumber(),
                resultado.getTotalElements());
    }

    private static Usuario aDominio(UsuarioEntity e) {
        return Usuario.reconstituir(e.getId(), e.getNombre(), e.getCorreo(), e.getContrasenaCifrada(), e.getTelefono(),
                e.getDocumento(), e.getFechaNacimiento(), e.getFotoUrl(), e.getRol(), e.isEliminado());
    }
}
