package co.edu.uniquindio.sga.domain.usuario;

import co.edu.uniquindio.sga.domain.comun.Pagina;

import java.util.Optional;

/** Puerto de persistencia de usuarios. */
public interface UsuarioRepositorio {

    void guardar(Usuario usuario);

    Optional<Usuario> buscar(String id);

    /** Busca por correo entre los usuarios no eliminados. */
    Optional<Usuario> buscarPorCorreo(String correo);

    boolean existeCorreo(String correo);

    Pagina<Usuario> listar(int pagina);
}
