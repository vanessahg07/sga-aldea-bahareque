package co.edu.uniquindio.sga.domain.apartamento;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import co.edu.uniquindio.sga.domain.tarifa.CalendarioTemporadas;
import co.edu.uniquindio.sga.domain.tarifa.TablaTarifas;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static co.edu.uniquindio.sga.domain.apartamento.EstadoOperativo.*;

/**
 * Unidad vendible del alojamiento: vivienda autónoma entregada en exclusiva a un grupo.
 * Controla su capacidad, sus imágenes y las transiciones de su estado operativo.
 */
public class Apartamento {

    public static final int MAXIMO_IMAGENES = 10;

    private final String codigo;
    private String nombre;
    private String descripcion;
    private int dormitorios;
    private int capacidad;
    private Set<String> caracteristicas;
    private boolean admiteMascotas;
    private List<ImagenApartamento> imagenes;
    private boolean activo;
    private boolean eliminado;
    private EstadoOperativo estadoOperativo;

    private Apartamento(String codigo, String nombre, String descripcion, int dormitorios, int capacidad,
                        Set<String> caracteristicas, boolean admiteMascotas) {
        ExcepcionNegocio.exigir(codigo != null && !codigo.isBlank(), CodigoError.APARTAMENTO_INVALIDO, "código");
        this.codigo = codigo.trim().toUpperCase();
        actualizarDatos(nombre, descripcion, dormitorios, caracteristicas, admiteMascotas);
        cambiarCapacidad(capacidad);
        this.imagenes = List.of();
        this.activo = false;
        this.eliminado = false;
        this.estadoOperativo = PREPARADO;
    }

    /** Registra un apartamento nuevo: queda inactivo, sin imágenes y PREPARADO. */
    public static Apartamento crear(String codigo, String nombre, String descripcion, int dormitorios, int capacidad,
                                    Set<String> caracteristicas, boolean admiteMascotas) {
        return new Apartamento(codigo, nombre, descripcion, dormitorios, capacidad, caracteristicas, admiteMascotas);
    }

    /** Reconstruye un apartamento ya existente, tal como fue guardado. */
    public static Apartamento reconstituir(String codigo, String nombre, String descripcion, int dormitorios,
                                           int capacidad, Set<String> caracteristicas, boolean admiteMascotas,
                                           List<ImagenApartamento> imagenes, boolean activo, boolean eliminado,
                                           EstadoOperativo estadoOperativo) {
        Apartamento apartamento = new Apartamento(codigo, nombre, descripcion, dormitorios, capacidad,
                caracteristicas, admiteMascotas);
        apartamento.imagenes = List.copyOf(imagenes);
        apartamento.activo = activo;
        apartamento.eliminado = eliminado;
        apartamento.estadoOperativo = estadoOperativo;
        return apartamento;
    }

    // --- Datos descriptivos ---

    public void actualizarDatos(String nombre, String descripcion, int dormitorios, Set<String> caracteristicas,
                                boolean admiteMascotas) {
        ExcepcionNegocio.exigir(nombre != null && !nombre.isBlank(), CodigoError.APARTAMENTO_INVALIDO, "nombre");
        ExcepcionNegocio.exigir(dormitorios >= 1, CodigoError.APARTAMENTO_INVALIDO, "dormitorios");
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.dormitorios = dormitorios;
        this.caracteristicas = caracteristicas == null ? Set.of() : Set.copyOf(caracteristicas);
        this.admiteMascotas = admiteMascotas;
    }

    /**
     * Cambiar la capacidad no afecta reservas ya creadas; la advertencia sobre las
     * reservas que quedan por encima es responsabilidad del caso de uso.
     */
    public void cambiarCapacidad(int nuevaCapacidad) {
        ExcepcionNegocio.exigir(nuevaCapacidad >= 1, CodigoError.APARTAMENTO_INVALIDO, "capacidad");
        this.capacidad = nuevaCapacidad;
    }

    /** Hasta 10 imágenes, con exactamente una principal. */
    public void definirImagenes(List<ImagenApartamento> nuevasImagenes) {
        ExcepcionNegocio.exigir(nuevasImagenes != null && !nuevasImagenes.isEmpty()
                && nuevasImagenes.size() <= MAXIMO_IMAGENES, CodigoError.IMAGENES_INVALIDAS);
        ExcepcionNegocio.exigir(nuevasImagenes.stream().filter(ImagenApartamento::principal).count() == 1,
                CodigoError.IMAGENES_INVALIDAS, "debe haber exactamente una imagen principal");
        this.imagenes = List.copyOf(nuevasImagenes);
    }

    public Optional<ImagenApartamento> imagenPrincipal() {
        return imagenes.stream().filter(ImagenApartamento::principal).findFirst();
    }

    // --- Venta ---

    /** Un apartamento solo se activa si tiene al menos una imagen y tarifa en todas las temporadas. */
    public void activar(TablaTarifas tarifas, CalendarioTemporadas calendario) {
        ExcepcionNegocio.exigir(!eliminado, CodigoError.APARTAMENTO_INACTIVO);
        ExcepcionNegocio.exigir(!imagenes.isEmpty(), CodigoError.IMAGENES_INVALIDAS, "se requiere al menos una imagen");
        ExcepcionNegocio.exigir(tarifas.tieneTarifasCompletas(codigo, calendario), CodigoError.TARIFAS_INCOMPLETAS, codigo);
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }

    /** Eliminación lógica: solo si no tiene reservas activas ni futuras. */
    public void eliminar(boolean tieneReservasActivasOFuturas) {
        ExcepcionNegocio.exigir(!tieneReservasActivasOFuturas, CodigoError.APARTAMENTO_CON_RESERVAS_ACTIVAS);
        this.eliminado = true;
        this.activo = false;
    }

    public boolean estaALaVenta() {
        return activo && !eliminado;
    }

    /** RN-02: la capacidad es un tope rígido, sin excepciones. */
    public void validarCapacidad(int totalOcupantes) {
        ExcepcionNegocio.exigir(totalOcupantes <= capacidad, CodigoError.CAPACIDAD_EXCEDIDA,
                totalOcupantes + " ocupantes en capacidad " + capacidad);
    }

    // --- Estado operativo ---

    /** RN-11: solo se recibe un grupo si el apartamento está PREPARADO. */
    public boolean puedeRecibirGrupo() {
        return estadoOperativo == PREPARADO;
    }

    /** Registro del grupo: PREPARADO → OCUPADO. */
    public void recibirGrupo() {
        ExcepcionNegocio.exigir(puedeRecibirGrupo(), CodigoError.APARTAMENTO_NO_PREPARADO, estadoOperativo.name());
        estadoOperativo = OCUPADO;
    }

    /** Salida del grupo: OCUPADO → PENDIENTE_PREPARACION. */
    public void liberar() {
        transitar(OCUPADO, PENDIENTE_PREPARACION);
    }

    /** Personal de servicio: PENDIENTE_PREPARACION → EN_PREPARACION. */
    public void iniciarPreparacion() {
        transitar(PENDIENTE_PREPARACION, EN_PREPARACION);
    }

    /** Personal de servicio: EN_PREPARACION → PREPARADO. */
    public void terminarPreparacion() {
        transitar(EN_PREPARACION, PREPARADO);
    }

    /**
     * Cambio de estado solicitado por el personal de servicio. Solo puede mover el
     * apartamento dentro del ciclo de preparación; nunca declararlo fuera de servicio.
     */
    public void cambiarEstadoPorPersonalDeServicio(EstadoOperativo destino) {
        switch (destino) {
            case EN_PREPARACION -> iniciarPreparacion();
            case PREPARADO -> terminarPreparacion();
            default -> throw new ExcepcionNegocio(CodigoError.OPERACION_NO_PERMITIDA_PERSONAL_SERVICIO, destino.name());
        }
    }

    /**
     * Cambio de estado solicitado por el administrador: además del ciclo de preparación,
     * puede declarar el apartamento fuera de servicio o rehabilitarlo.
     */
    public void cambiarEstadoPorAdministrador(EstadoOperativo destino) {
        switch (destino) {
            case FUERA_DE_SERVICIO -> declararFueraDeServicio();
            case PENDIENTE_PREPARACION -> rehabilitar();
            case EN_PREPARACION, PREPARADO -> cambiarEstadoPorPersonalDeServicio(destino);
            case OCUPADO -> throw new ExcepcionNegocio(CodigoError.TRANSICION_ESTADO_OPERATIVO_INVALIDA,
                    "un apartamento solo pasa a OCUPADO al registrar la llegada de un grupo");
        }
    }

    /** Solo administrador: desde cualquier estado no ocupado. */
    public void declararFueraDeServicio() {
        ExcepcionNegocio.exigir(estadoOperativo != OCUPADO, CodigoError.TRANSICION_ESTADO_OPERATIVO_INVALIDA,
                "un apartamento ocupado no puede declararse fuera de servicio");
        estadoOperativo = FUERA_DE_SERVICIO;
    }

    /** Solo administrador: FUERA_DE_SERVICIO → PENDIENTE_PREPARACION. */
    public void rehabilitar() {
        transitar(FUERA_DE_SERVICIO, PENDIENTE_PREPARACION);
    }

    private void transitar(EstadoOperativo desde, EstadoOperativo hacia) {
        ExcepcionNegocio.exigir(estadoOperativo == desde, CodigoError.TRANSICION_ESTADO_OPERATIVO_INVALIDA,
                estadoOperativo + " → " + hacia);
        estadoOperativo = hacia;
    }

    // --- Consultas ---

    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public int getDormitorios() { return dormitorios; }
    public int getCapacidad() { return capacidad; }
    public Set<String> getCaracteristicas() { return caracteristicas; }
    public boolean admiteMascotas() { return admiteMascotas; }
    public List<ImagenApartamento> getImagenes() { return imagenes; }
    public boolean isActivo() { return activo; }
    public boolean isEliminado() { return eliminado; }
    public EstadoOperativo getEstadoOperativo() { return estadoOperativo; }

    // --- Identidad: dos apartamentos son el mismo si tienen el mismo código ---

    @Override
    public boolean equals(Object otro) {
        return this == otro || otro instanceof Apartamento a && codigo.equals(a.codigo);
    }

    @Override
    public int hashCode() {
        return codigo.hashCode();
    }
}
