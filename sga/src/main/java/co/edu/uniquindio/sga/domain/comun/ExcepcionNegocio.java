package co.edu.uniquindio.sga.domain.comun;

/**
 * Violación de una regla de negocio. Lleva un código estable que los adaptadores
 * traducen a una respuesta con significado de negocio.
 */
public class ExcepcionNegocio extends RuntimeException {

    private final CodigoError codigo;

    public ExcepcionNegocio(CodigoError codigo) {
        super(codigo.mensaje());
        this.codigo = codigo;
    }

    public ExcepcionNegocio(CodigoError codigo, String detalle) {
        super(codigo.mensaje() + ": " + detalle);
        this.codigo = codigo;
    }

    public CodigoError getCodigo() {
        return codigo;
    }

    /** Lanza la excepción si la condición no se cumple. */
    public static void exigir(boolean condicion, CodigoError codigo) {
        if (!condicion) {
            throw new ExcepcionNegocio(codigo);
        }
    }

    public static void exigir(boolean condicion, CodigoError codigo, String detalle) {
        if (!condicion) {
            throw new ExcepcionNegocio(codigo, detalle);
        }
    }
}
