package co.edu.uniquindio.sga.domain.usuario;

/** Roles de acceso al sistema. */
public enum Rol {
    HUESPED,
    RECEPCIONISTA,
    ADMINISTRADOR,
    PERSONAL_SERVICIO;

    public boolean esInterno() {
        return this != HUESPED;
    }
}
