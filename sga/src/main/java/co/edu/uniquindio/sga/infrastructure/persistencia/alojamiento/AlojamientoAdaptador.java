package co.edu.uniquindio.sga.infrastructure.persistencia.alojamiento;

import co.edu.uniquindio.sga.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga.domain.alojamiento.AlojamientoRepositorio;
import co.edu.uniquindio.sga.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga.domain.alojamiento.ServicioAdicional;
import co.edu.uniquindio.sga.domain.comun.Dinero;
import co.edu.uniquindio.sga.domain.folio.MedioPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.Collectors;

/** Adaptador JPA del puerto {@link AlojamientoRepositorio}. */
@Repository
@Transactional
public class AlojamientoAdaptador implements AlojamientoRepositorio {

    /** Días del recordatorio de llegada de la Ficha (A.4) para bases creadas antes del Corte 4. */
    private static final int DIAS_RECORDATORIO_POR_DEFECTO = 2;
    /** Código de un alojamiento guardado antes de que el código existiera. */
    private static final String CODIGO_POR_DEFECTO = "ALDEA-BAHAREQUE";

    public interface Jpa extends JpaRepository<AlojamientoEntity, Long> {
    }

    private final Jpa jpa;

    public AlojamientoAdaptador(Jpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Alojamiento> obtener() {
        return jpa.findById(AlojamientoEntity.ID_UNICO).map(AlojamientoAdaptador::aDominio);
    }

    @Override
    public void guardar(Alojamiento alojamiento) {
        AlojamientoEntity e = jpa.findById(AlojamientoEntity.ID_UNICO).orElseGet(AlojamientoEntity::new);
        e.setCodigo(alojamiento.getCodigo());
        e.setNombre(alojamiento.getNombre());
        e.setDescripcion(alojamiento.getDescripcion());
        e.setCiudad(alojamiento.getCiudad());
        e.setDireccion(alojamiento.getDireccion());
        e.setLatitud(alojamiento.getLatitud());
        e.setLongitud(alojamiento.getLongitud());
        e.getNormas().clear();
        e.getNormas().addAll(alojamiento.getNormas());
        e.getServicios().clear();
        alojamiento.getServicios().forEach(s -> e.getServicios().add(new AlojamientoEntity.Servicio(
                s.codigo(), s.nombre(), s.descripcion(), s.generaCargo(), s.valor().valor(), s.unidad())));
        e.getMediosPago().clear();
        alojamiento.getMediosPago().forEach(m -> e.getMediosPago().add(m.nombre()));
        ParametrosAlojamiento p = alojamiento.getParametros();
        e.setUmbralEdadFacturable(p.umbralEdadFacturable());
        e.setHoraEntrada(p.horaEntrada());
        e.setHoraSalida(p.horaSalida());
        e.setHorasPreparacion(p.horasPreparacion());
        e.setHorasPlazoConfirmacion(p.horasPlazoConfirmacion());
        e.setHoraLimiteNoShow(p.horaLimiteNoShow());
        e.setPorcentajeAnticipo(p.porcentajeAnticipo());
        e.setMaxMascotas(p.maxMascotas());
        e.setCargoMascota(p.cargoMascota().valor());
        e.setNochesDescuentoLargo(p.nochesDescuentoLargo());
        e.setPorcentajeDescuentoLargo(p.porcentajeDescuentoLargo());
        e.setDiasRecordatorioLlegada(p.diasRecordatorioLlegada());
        jpa.save(e);
    }

    private static Alojamiento aDominio(AlojamientoEntity e) {
        ParametrosAlojamiento parametros = new ParametrosAlojamiento(e.getUmbralEdadFacturable(), e.getHoraEntrada(),
                e.getHoraSalida(), e.getHorasPreparacion(), e.getHorasPlazoConfirmacion(), e.getHoraLimiteNoShow(),
                e.getPorcentajeAnticipo(), e.getMaxMascotas(), Dinero.de(e.getCargoMascota()),
                e.getNochesDescuentoLargo(), e.getPorcentajeDescuentoLargo(),
                e.getDiasRecordatorioLlegada() == null ? DIAS_RECORDATORIO_POR_DEFECTO : e.getDiasRecordatorioLlegada());
        return Alojamiento.crear(e.getCodigo() == null ? CODIGO_POR_DEFECTO : e.getCodigo(), e.getNombre(), e.getDescripcion(), e.getCiudad(), e.getDireccion(), e.getLatitud(),
                e.getLongitud(), e.getNormas(),
                e.getServicios().stream().map(s -> new ServicioAdicional(s.getCodigo(), s.getNombre(),
                        s.getDescripcion(), s.isGeneraCargo(), Dinero.de(s.getValor()), s.getUnidad())).toList(),
                e.getMediosPago().stream().map(MedioPago::new).collect(Collectors.toSet()), parametros);
    }
}
