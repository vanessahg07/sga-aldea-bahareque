package co.edu.uniquindio.sga.application.apartamento;

import co.edu.uniquindio.sga.application.comun.Solicitante;
import co.edu.uniquindio.sga.domain.apartamento.*;
import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import co.edu.uniquindio.sga.domain.comun.Pagina;
import co.edu.uniquindio.sga.domain.comun.RangoFechas;
import co.edu.uniquindio.sga.domain.comun.Reloj;
import co.edu.uniquindio.sga.domain.reserva.Reserva;
import co.edu.uniquindio.sga.domain.reserva.ReservaRepositorio;
import co.edu.uniquindio.sga.domain.tarifa.TarifaRepositorio;
import co.edu.uniquindio.sga.domain.tarifa.TemporadaRepositorio;
import co.edu.uniquindio.sga.domain.usuario.Rol;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Casos de uso del inventario: apartamentos, imágenes, bloqueos, estado operativo y novedades. */
@Service
@Transactional
public class ApartamentoServicio {

    /** Datos editables de un apartamento. */
    public record DatosApartamento(String nombre, String descripcion, int dormitorios, int capacidad,
                                   Set<String> caracteristicas, boolean admiteMascotas) {
    }

    /** Resultado de actualizar: las reservas activas que quedaron por encima de la nueva capacidad. */
    public record ResultadoActualizacion(Apartamento apartamento, List<Reserva> reservasSobreCapacidad) {
    }

    public enum EstadoNoche { DISPONIBLE, RESERVADA, BLOQUEADA, NO_A_LA_VENTA }

    public record Noche(LocalDate fecha, EstadoNoche estado) {
    }

    private final ApartamentoRepositorio apartamentos;
    private final BloqueoRepositorio bloqueos;
    private final NovedadRepositorio novedades;
    private final ReservaRepositorio reservas;
    private final TarifaRepositorio tarifas;
    private final TemporadaRepositorio temporadas;
    private final Reloj reloj;

    public ApartamentoServicio(ApartamentoRepositorio apartamentos, BloqueoRepositorio bloqueos,
                               NovedadRepositorio novedades, ReservaRepositorio reservas, TarifaRepositorio tarifas,
                               TemporadaRepositorio temporadas, Reloj reloj) {
        this.apartamentos = apartamentos;
        this.bloqueos = bloqueos;
        this.novedades = novedades;
        this.reservas = reservas;
        this.tarifas = tarifas;
        this.temporadas = temporadas;
        this.reloj = reloj;
    }

    // --- Consultas ---

    @Transactional(readOnly = true)
    public Apartamento consultar(String codigo) {
        return apartamentos.buscar(codigo)
                .filter(a -> !a.isEliminado())
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.APARTAMENTO_NO_ENCONTRADO, codigo));
    }

    /** Listado administrativo: todos los apartamentos no eliminados, estén o no a la venta. */
    @Transactional(readOnly = true)
    public Pagina<Apartamento> listar(int pagina) {
        return Pagina.de(ordenados(apartamentos.listarNoEliminados()), pagina);
    }

    /** Listado público: solo los apartamentos a la venta. */
    @Transactional(readOnly = true)
    public Pagina<Apartamento> listarALaVenta(int pagina) {
        return Pagina.de(ordenados(apartamentos.listarNoEliminados()).stream()
                .filter(Apartamento::estaALaVenta).toList(), pagina);
    }

    /** Calendario de disponibilidad noche por noche en [desde, hasta). */
    @Transactional(readOnly = true)
    public List<Noche> calendario(String codigo, LocalDate desde, LocalDate hasta) {
        Apartamento apartamento = consultar(codigo);
        RangoFechas rango = new RangoFechas(desde, hasta);
        List<Reserva> activas = reservas.activasDe(apartamento.getCodigo());
        List<Bloqueo> vigentes = bloqueos.vigentesDe(apartamento.getCodigo());
        return rango.listaNoches().stream().map(noche -> {
            if (!apartamento.estaALaVenta()) return new Noche(noche, EstadoNoche.NO_A_LA_VENTA);
            if (activas.stream().anyMatch(r -> r.getEstancia().rango().contiene(noche)))
                return new Noche(noche, EstadoNoche.RESERVADA);
            if (vigentes.stream().anyMatch(b -> b.rango().contiene(noche)))
                return new Noche(noche, EstadoNoche.BLOQUEADA);
            return new Noche(noche, EstadoNoche.DISPONIBLE);
        }).toList();
    }

    // --- Gestión ---

    /** Crea el apartamento inactivo: se activa cuando tenga imágenes y tarifas completas. */
    public Apartamento crear(String codigo, DatosApartamento datos) {
        ExcepcionNegocio.exigir(!apartamentos.existe(codigo), CodigoError.APARTAMENTO_YA_EXISTE, codigo);
        Apartamento apartamento = Apartamento.crear(codigo, datos.nombre(), datos.descripcion(), datos.dormitorios(),
                datos.capacidad(), datos.caracteristicas(), datos.admiteMascotas());
        apartamentos.guardar(apartamento);
        return apartamento;
    }

    /**
     * Actualiza los datos. Reducir la capacidad no afecta las reservas ya creadas, pero
     * se informan las que quedan por encima para que el administrador las atienda.
     */
    public ResultadoActualizacion actualizar(String codigo, DatosApartamento datos) {
        Apartamento apartamento = consultar(codigo);
        apartamento.actualizarDatos(datos.nombre(), datos.descripcion(), datos.dormitorios(),
                datos.caracteristicas(), datos.admiteMascotas());
        apartamento.cambiarCapacidad(datos.capacidad());
        apartamentos.guardar(apartamento);
        List<Reserva> sobreCapacidad = reservas.activasDe(apartamento.getCodigo()).stream()
                .filter(r -> r.ocupantes().size() > apartamento.getCapacidad())
                .toList();
        return new ResultadoActualizacion(apartamento, sobreCapacidad);
    }

    public Apartamento definirImagenes(String codigo, List<ImagenApartamento> imagenes) {
        Apartamento apartamento = consultar(codigo);
        apartamento.definirImagenes(imagenes);
        apartamentos.guardar(apartamento);
        return apartamento;
    }

    public Apartamento activar(String codigo) {
        Apartamento apartamento = consultar(codigo);
        apartamento.activar(tarifas.tabla(), temporadas.calendario());
        apartamentos.guardar(apartamento);
        return apartamento;
    }

    public Apartamento desactivar(String codigo) {
        Apartamento apartamento = consultar(codigo);
        apartamento.desactivar();
        apartamentos.guardar(apartamento);
        return apartamento;
    }

    /** Retiro de la venta con eliminación lógica, solo si no tiene reservas activas ni futuras. */
    public void eliminar(String codigo) {
        Apartamento apartamento = consultar(codigo);
        apartamento.eliminar(!reservas.activasDe(apartamento.getCodigo()).isEmpty());
        apartamentos.guardar(apartamento);
    }

    // --- Bloqueos ---

    public Bloqueo registrarBloqueo(String codigo, LocalDate desde, LocalDate hasta, String motivo) {
        Apartamento apartamento = apartamentos.buscarParaActualizar(codigo)
                .filter(a -> !a.isEliminado())
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.APARTAMENTO_NO_ENCONTRADO, codigo));
        List<RangoFechas> ocupadas = reservas.activasDe(apartamento.getCodigo()).stream()
                .map(r -> r.getEstancia().rango()).toList();
        Bloqueo bloqueo = Bloqueo.registrar(apartamento.getCodigo(), new RangoFechas(desde, hasta), motivo, ocupadas);
        bloqueos.guardar(bloqueo);
        return bloqueo;
    }

    @Transactional(readOnly = true)
    public List<Bloqueo> bloqueosVigentes(String codigo) {
        return bloqueos.vigentesDe(consultar(codigo).getCodigo());
    }

    public void levantarBloqueo(String idBloqueo) {
        bloqueos.buscarVigente(idBloqueo).orElseThrow(() -> new ExcepcionNegocio(CodigoError.BLOQUEO_NO_ENCONTRADO));
        bloqueos.levantar(idBloqueo);
    }

    // --- Estado operativo ---

    /**
     * El personal de servicio solo mueve el ciclo de preparación; el administrador además
     * declara fuera de servicio y rehabilita. El registro y la salida cambian el estado
     * desde los casos de uso de reservas.
     */
    public Apartamento cambiarEstadoOperativo(String codigo, EstadoOperativo destino, Solicitante solicitante) {
        Apartamento apartamento = consultar(codigo);
        if (solicitante.rol() == Rol.ADMINISTRADOR) {
            apartamento.cambiarEstadoPorAdministrador(destino);
        } else if (solicitante.rol() == Rol.PERSONAL_SERVICIO) {
            apartamento.cambiarEstadoPorPersonalDeServicio(destino);
        } else {
            throw new ExcepcionNegocio(CodigoError.ACCESO_DENEGADO);
        }
        apartamentos.guardar(apartamento);
        return apartamento;
    }

    @Transactional(readOnly = true)
    public List<Apartamento> pendientesDePreparacion() {
        return ordenados(apartamentos.listarNoEliminados()).stream()
                .filter(a -> a.getEstadoOperativo() == EstadoOperativo.PENDIENTE_PREPARACION
                        || a.getEstadoOperativo() == EstadoOperativo.EN_PREPARACION)
                .toList();
    }

    // --- Novedades ---

    public Novedad registrarNovedad(String codigo, String descripcion, Novedad.Gravedad gravedad,
                                    Solicitante autor) {
        Apartamento apartamento = consultar(codigo);
        Novedad novedad = Novedad.registrar(apartamento.getCodigo(), reloj.ahora(), autor.nombre(), descripcion,
                gravedad);
        novedades.guardar(novedad);
        return novedad;
    }

    @Transactional(readOnly = true)
    public Pagina<Novedad> historialNovedades(String codigo, int pagina) {
        return novedades.deApartamento(consultar(codigo).getCodigo(), pagina);
    }

    private static List<Apartamento> ordenados(List<Apartamento> lista) {
        return lista.stream().sorted(Comparator.comparing(Apartamento::getCodigo)).toList();
    }
}
