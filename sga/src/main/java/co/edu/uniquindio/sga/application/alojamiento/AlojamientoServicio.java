package co.edu.uniquindio.sga.application.alojamiento;

import co.edu.uniquindio.sga.application.comun.ContextoVigente;
import co.edu.uniquindio.sga.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga.domain.alojamiento.AlojamientoRepositorio;
import co.edu.uniquindio.sga.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga.domain.alojamiento.ServicioAdicional;
import co.edu.uniquindio.sga.domain.folio.MedioPago;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/** Casos de uso sobre los datos, normas, servicios y parámetros del alojamiento. */
@Service
@Transactional
public class AlojamientoServicio {

    private final AlojamientoRepositorio alojamientos;
    private final ContextoVigente contexto;

    public AlojamientoServicio(AlojamientoRepositorio alojamientos, ContextoVigente contexto) {
        this.alojamientos = alojamientos;
        this.contexto = contexto;
    }

    @Transactional(readOnly = true)
    public Alojamiento consultar() {
        return contexto.alojamiento();
    }

    public Alojamiento actualizarDatos(String nombre, String descripcion, String ciudad, String direccion,
                                      BigDecimal latitud, BigDecimal longitud, List<String> normas) {
        Alojamiento alojamiento = contexto.alojamiento();
        alojamiento.actualizarDatos(nombre, descripcion, ciudad, direccion, latitud, longitud, normas);
        alojamientos.guardar(alojamiento);
        return alojamiento;
    }

    public Alojamiento cambiarParametros(ParametrosAlojamiento parametros) {
        Alojamiento alojamiento = contexto.alojamiento();
        alojamiento.cambiarParametros(parametros);
        alojamientos.guardar(alojamiento);
        return alojamiento;
    }

    public Alojamiento definirServicios(List<ServicioAdicional> servicios) {
        Alojamiento alojamiento = contexto.alojamiento();
        alojamiento.definirServicios(servicios);
        alojamientos.guardar(alojamiento);
        return alojamiento;
    }

    public Alojamiento definirMediosPago(Set<MedioPago> medios) {
        Alojamiento alojamiento = contexto.alojamiento();
        alojamiento.definirMediosPago(medios);
        alojamientos.guardar(alojamiento);
        return alojamiento;
    }
}
