package co.edu.uniquindio.sga.domain.alojamiento;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import co.edu.uniquindio.sga.domain.folio.MedioPago;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * El negocio completo. El sistema administra un único alojamiento: sus datos, normas,
 * servicios adicionales, medios de pago aceptados y parámetros configurables.
 */
public class Alojamiento {

    private final String codigo;
    private String nombre;
    private String descripcion;
    private String ciudad;
    private String direccion;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private List<String> normas;
    private List<ServicioAdicional> servicios;
    private Set<MedioPago> mediosPago;
    private ParametrosAlojamiento parametros;

    private Alojamiento(String codigo, String nombre, String descripcion, String ciudad, String direccion,
                        BigDecimal latitud, BigDecimal longitud, List<String> normas, List<ServicioAdicional> servicios,
                        Set<MedioPago> mediosPago, ParametrosAlojamiento parametros) {
        ExcepcionNegocio.exigir(codigo != null && !codigo.isBlank(), CodigoError.ALOJAMIENTO_INVALIDO, "código");
        this.codigo = codigo.trim().toUpperCase();
        actualizarDatos(nombre, descripcion, ciudad, direccion, latitud, longitud, normas);
        definirServicios(servicios);
        definirMediosPago(mediosPago);
        cambiarParametros(parametros);
    }

    /** Crea el alojamiento con todos sus datos; el código lo identifica y no cambia. */
    public static Alojamiento crear(String codigo, String nombre, String descripcion, String ciudad, String direccion,
                                    BigDecimal latitud, BigDecimal longitud, List<String> normas,
                                    List<ServicioAdicional> servicios, Set<MedioPago> mediosPago,
                                    ParametrosAlojamiento parametros) {
        return new Alojamiento(codigo, nombre, descripcion, ciudad, direccion, latitud, longitud, normas, servicios,
                mediosPago, parametros);
    }

    public void actualizarDatos(String nombre, String descripcion, String ciudad, String direccion,
                                BigDecimal latitud, BigDecimal longitud, List<String> normas) {
        ExcepcionNegocio.exigir(nombre != null && !nombre.isBlank(), CodigoError.ALOJAMIENTO_INVALIDO, "nombre");
        ExcepcionNegocio.exigir(ciudad != null && !ciudad.isBlank(), CodigoError.ALOJAMIENTO_INVALIDO, "ciudad");
        ExcepcionNegocio.exigir(latitud != null && latitud.abs().compareTo(BigDecimal.valueOf(90)) <= 0,
                CodigoError.ALOJAMIENTO_INVALIDO, "latitud");
        ExcepcionNegocio.exigir(longitud != null && longitud.abs().compareTo(BigDecimal.valueOf(180)) <= 0,
                CodigoError.ALOJAMIENTO_INVALIDO, "longitud");
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.ciudad = ciudad;
        this.direccion = direccion;
        this.latitud = latitud;
        this.longitud = longitud;
        this.normas = normas == null ? List.of() : List.copyOf(normas);
    }

    public void definirServicios(List<ServicioAdicional> nuevos) {
        List<ServicioAdicional> lista = nuevos == null ? List.of() : List.copyOf(nuevos);
        ExcepcionNegocio.exigir(lista.stream().map(ServicioAdicional::codigo).distinct().count() == lista.size(),
                CodigoError.ALOJAMIENTO_INVALIDO, "códigos de servicio repetidos");
        this.servicios = lista;
    }

    /** L-18: se aceptan al menos dos medios de pago. */
    public void definirMediosPago(Set<MedioPago> medios) {
        ExcepcionNegocio.exigir(medios != null && medios.size() >= 2, CodigoError.ALOJAMIENTO_INVALIDO,
                "se requieren al menos dos medios de pago");
        this.mediosPago = Set.copyOf(medios);
    }

    public void cambiarParametros(ParametrosAlojamiento nuevos) {
        ExcepcionNegocio.exigir(nuevos != null, CodigoError.PARAMETROS_INVALIDOS);
        this.parametros = nuevos;
    }

    public void validarMedioPago(MedioPago medio) {
        ExcepcionNegocio.exigir(medio != null && mediosPago.contains(medio), CodigoError.MEDIO_PAGO_NO_ACEPTADO,
                medio == null ? "sin medio" : medio.nombre());
    }

    public ServicioAdicional servicio(String codigo) {
        return servicios.stream().filter(s -> s.codigo().equalsIgnoreCase(codigo)).findFirst()
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.SERVICIO_NO_ENCONTRADO, codigo));
    }

    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getCiudad() { return ciudad; }
    public String getDireccion() { return direccion; }
    public BigDecimal getLatitud() { return latitud; }
    public BigDecimal getLongitud() { return longitud; }
    public List<String> getNormas() { return normas; }
    public List<ServicioAdicional> getServicios() { return servicios; }
    public Set<MedioPago> getMediosPago() { return mediosPago; }
    public ParametrosAlojamiento getParametros() { return parametros; }

    // --- Identidad: dos alojamientos son el mismo si tienen el mismo código ---

    @Override
    public boolean equals(Object otro) {
        return this == otro || otro instanceof Alojamiento a && codigo.equals(a.codigo);
    }

    @Override
    public int hashCode() {
        return codigo.hashCode();
    }
}
