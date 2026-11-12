package co.edu.uniquindio.sga.application.canal;

import co.edu.uniquindio.sga.application.comun.Solicitante;
import co.edu.uniquindio.sga.domain.canal.*;
import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;
import co.edu.uniquindio.sga.domain.comun.Pagina;
import co.edu.uniquindio.sga.domain.comun.Reloj;
import co.edu.uniquindio.sga.domain.usuario.CifradorContrasenas;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Administración de los canales de venta, sus conflictos y su bitácora. */
@Service
@Transactional
public class CanalServicio {

    private final CanalVentaRepositorio canales;
    private final ConflictoCanalRepositorio conflictos;
    private final BitacoraCanalRepositorio bitacora;
    private final CifradorContrasenas cifrador;
    private final Reloj reloj;

    public CanalServicio(CanalVentaRepositorio canales, ConflictoCanalRepositorio conflictos,
                         BitacoraCanalRepositorio bitacora, CifradorContrasenas cifrador, Reloj reloj) {
        this.canales = canales;
        this.conflictos = conflictos;
        this.bitacora = bitacora;
        this.cifrador = cifrador;
        this.reloj = reloj;
    }

    // --- Canales ---

    /** Registra el canal y devuelve su credencial en claro, que no vuelve a mostrarse. */
    public CanalVenta.Credencial registrar(String codigo, String nombre, TipoCanal tipo) {
        ExcepcionNegocio.exigir(codigo == null || !canales.existe(codigo.trim()), CodigoError.CANAL_YA_EXISTE);
        CanalVenta.Credencial credencial = CanalVenta.registrar(codigo == null ? null : codigo.trim(), nombre, tipo,
                cifrador);
        canales.guardar(credencial.canal());
        return credencial;
    }

    public CanalVenta actualizar(String codigo, String nombre, TipoCanal tipo) {
        CanalVenta canal = buscar(codigo);
        canal.actualizarDatos(nombre, tipo);
        canales.guardar(canal);
        return canal;
    }

    public CanalVenta.Credencial renovarCredencial(String codigo) {
        CanalVenta canal = buscar(codigo);
        String nueva = canal.renovarCredencial(cifrador);
        canales.guardar(canal);
        return new CanalVenta.Credencial(canal, nueva);
    }

    public CanalVenta cambiarActivo(String codigo, boolean activo) {
        CanalVenta canal = buscar(codigo);
        if (activo) {
            canal.activar();
        } else {
            canal.desactivar();
        }
        canales.guardar(canal);
        return canal;
    }

    @Transactional(readOnly = true)
    public List<CanalVenta> listar() {
        return canales.listar();
    }

    // --- Conflictos (RN-18) ---

    @Transactional(readOnly = true)
    public Pagina<ConflictoCanal> conflictos(boolean soloPendientes, int pagina) {
        return conflictos.listar(soloPendientes, pagina);
    }

    /** El administrador deja constancia de cómo resolvió el conflicto (por ejemplo, reubicó o avisó al canal). */
    public ConflictoCanal revisarConflicto(String id, String nota, Solicitante administrador) {
        administrador.exigirAdministrador();
        ConflictoCanal conflicto = conflictos.buscar(id)
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.CONFLICTO_NO_ENCONTRADO, id));
        ConflictoCanal revisado = conflicto.revisar(administrador.nombre(), nota, reloj.ahora());
        conflictos.guardar(revisado);
        return revisado;
    }

    // --- Bitácora ---

    @Transactional(readOnly = true)
    public Pagina<EventoCanal> bitacora(String codigoCanal, int pagina) {
        return bitacora.listar(codigoCanal == null || codigoCanal.isBlank() ? null : codigoCanal.trim(), pagina);
    }

    private CanalVenta buscar(String codigo) {
        return canales.buscar(codigo).orElseThrow(() -> new ExcepcionNegocio(CodigoError.CANAL_NO_ENCONTRADO, codigo));
    }
}
