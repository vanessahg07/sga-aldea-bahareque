package co.edu.uniquindio.sga.domain.folio;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import co.edu.uniquindio.sga.domain.comun.Dinero;
import co.edu.uniquindio.sga.domain.comun.ExcepcionNegocio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cuenta de una reserva: acumula cargos y pagos y determina el saldo.
 * Los movimientos nunca se editan ni se eliminan; se corrigen con movimientos inversos.
 */
public class Folio {

    private final String id;
    private final String idReserva;
    private final List<Cargo> cargos = new ArrayList<>();
    private final List<Pago> pagos = new ArrayList<>();
    private boolean cerrado;
    private AutorizacionCierre autorizacionCierre;

    private Folio(String id, String idReserva) {
        this.id = id;
        this.idReserva = idReserva;
    }

    /** Abre el folio vacío de una reserva nueva. */
    public static Folio abrir(String idReserva) {
        return new Folio(UUID.randomUUID().toString(), idReserva);
    }

    /** Reconstruye un folio ya existente con sus movimientos, tal como fue guardado. */
    public static Folio reconstituir(String id, String idReserva, List<Cargo> cargos, List<Pago> pagos,
                                     boolean cerrado, AutorizacionCierre autorizacion) {
        Folio folio = new Folio(id, idReserva);
        folio.cargos.addAll(cargos);
        folio.pagos.addAll(pagos);
        folio.cerrado = cerrado;
        folio.autorizacionCierre = autorizacion;
        return folio;
    }

    // --- Cargos ---

    public Cargo agregarCargo(TipoCargo tipo, Dinero valor, String descripcion, LocalDateTime fecha) {
        exigirAbierto();
        Cargo cargo = new Cargo(nuevoId(), tipo, valor, descripcion, fecha, null);
        cargos.add(cargo);
        return cargo;
    }

    /** RN-16: la corrección de un cargo es un cargo inverso. */
    public Cargo revertirCargo(String idCargo, String motivo, LocalDateTime fecha) {
        exigirAbierto();
        Cargo original = cargos.stream().filter(c -> c.id().equals(idCargo)).findFirst()
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.MOVIMIENTO_NO_ENCONTRADO));
        ExcepcionNegocio.exigir(!original.esReverso(), CodigoError.MOVIMIENTO_YA_REVERTIDO, "un reverso no se revierte");
        ExcepcionNegocio.exigir(!cargosRevertidos().contains(idCargo), CodigoError.MOVIMIENTO_YA_REVERTIDO);
        Cargo reverso = new Cargo(nuevoId(), original.tipo(), original.valor().negar(),
                "Reverso: " + motivo, fecha, idCargo);
        cargos.add(reverso);
        return reverso;
    }

    /**
     * Revierte todos los cargos vigentes. Se usa cuando la reserva termina sin
     * estancia (cancelación, no-show, vencimiento) antes de registrar la penalidad.
     */
    public void anularCargosVigentes(String motivo, LocalDateTime fecha) {
        Set<String> revertidos = cargosRevertidos();
        List<Cargo> vigentes = cargos.stream()
                .filter(c -> !c.esReverso() && !revertidos.contains(c.id()))
                .toList();
        vigentes.forEach(c -> revertirCargo(c.id(), motivo, fecha));
    }

    // --- Pagos ---

    /** RN-15: todo pago tiene valor positivo, medio y fecha. */
    public Pago registrarPago(Dinero valor, MedioPago medio, LocalDateTime fecha) {
        exigirAbierto();
        ExcepcionNegocio.exigir(valor != null && valor.esPositivo(), CodigoError.PAGO_INVALIDO, "valor");
        ExcepcionNegocio.exigir(medio != null, CodigoError.PAGO_INVALIDO, "medio de pago");
        ExcepcionNegocio.exigir(fecha != null, CodigoError.PAGO_INVALIDO, "fecha");
        Pago pago = new Pago(nuevoId(), valor, medio, fecha, null);
        pagos.add(pago);
        return pago;
    }

    /** RN-16: la corrección de un pago es un pago inverso. */
    public Pago revertirPago(String idPago, LocalDateTime fecha) {
        exigirAbierto();
        Pago original = pagos.stream().filter(p -> p.id().equals(idPago)).findFirst()
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.MOVIMIENTO_NO_ENCONTRADO));
        ExcepcionNegocio.exigir(!original.esReverso(), CodigoError.MOVIMIENTO_YA_REVERTIDO, "un reverso no se revierte");
        ExcepcionNegocio.exigir(pagos.stream().noneMatch(p -> idPago.equals(p.idPagoRevertido())),
                CodigoError.MOVIMIENTO_YA_REVERTIDO);
        Pago reverso = new Pago(nuevoId(), original.valor().negar(), original.medio(), fecha, idPago);
        pagos.add(reverso);
        return reverso;
    }

    // --- Saldo y cierre ---

    public Dinero totalCargos() {
        return cargos.stream().map(Cargo::valor).reduce(Dinero.CERO, Dinero::mas);
    }

    public Dinero totalPagos() {
        return pagos.stream().map(Pago::valor).reduce(Dinero.CERO, Dinero::mas);
    }

    /** Positivo: debe el huésped. Cero: a paz y salvo. Negativo: saldo a favor del huésped. */
    public Dinero saldo() {
        return totalCargos().menos(totalPagos());
    }

    /** RN-17: sin autorización solo se cierra con saldo cero. */
    public void cerrar() {
        exigirAbierto();
        ExcepcionNegocio.exigir(saldo().esCero(), CodigoError.FOLIO_CON_SALDO, saldo().toString());
        cerrado = true;
    }

    /** RN-17: cierre con saldo distinto de cero, con autorización registrada. */
    public void cerrarConAutorizacion(AutorizacionCierre autorizacion) {
        exigirAbierto();
        ExcepcionNegocio.exigir(autorizacion != null, CodigoError.AUTORIZACION_INVALIDA);
        this.autorizacionCierre = autorizacion;
        this.cerrado = true;
    }

    private void exigirAbierto() {
        ExcepcionNegocio.exigir(!cerrado, CodigoError.FOLIO_CERRADO);
    }

    private Set<String> cargosRevertidos() {
        return cargos.stream().map(Cargo::idCargoRevertido)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static String nuevoId() {
        return UUID.randomUUID().toString();
    }

    // --- Consultas ---

    public String getId() { return id; }
    public String getIdReserva() { return idReserva; }
    public List<Cargo> getCargos() { return Collections.unmodifiableList(cargos); }
    public List<Pago> getPagos() { return Collections.unmodifiableList(pagos); }
    public boolean isCerrado() { return cerrado; }
    public Optional<AutorizacionCierre> getAutorizacionCierre() { return Optional.ofNullable(autorizacionCierre); }

    // --- Identidad: dos folios son el mismo si tienen el mismo identificador ---

    @Override
    public boolean equals(Object otro) {
        return this == otro || otro instanceof Folio f && id.equals(f.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
