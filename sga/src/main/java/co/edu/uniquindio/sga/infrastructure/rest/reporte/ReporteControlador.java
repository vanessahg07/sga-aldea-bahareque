package co.edu.uniquindio.sga.infrastructure.rest.reporte;

import co.edu.uniquindio.sga.application.reporte.ReporteServicio;
import co.edu.uniquindio.sga.domain.reporte.Reportes;
import co.edu.uniquindio.sga.domain.reserva.Canal;
import co.edu.uniquindio.sga.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga.infrastructure.rest.comun.ErrorRespuesta;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static co.edu.uniquindio.sga.infrastructure.rest.comun.Respuestas.pesos;

/**
 * Reportes con filtro por periodo (fechas incluidas). Cada uno se puede descargar en CSV
 * para abrirlo en una hoja de cálculo (funcionalidad opcional 8).
 */
@RestController
@RequestMapping("/api/reportes")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ReporteControlador {

    public record OcupacionApartamentoR(String codigoApartamento, long nochesVendidas, BigDecimal porcentaje) {
    }

    public record OcupacionR(LocalDate desde, LocalDate hasta, int apartamentosActivos, long nochesDisponibles,
                             long nochesVendidas, BigDecimal porcentaje, List<OcupacionApartamentoR> porApartamento) {
        static OcupacionR de(Reportes.Ocupacion o) {
            return new OcupacionR(o.periodo().desde(), o.periodo().hasta(), o.apartamentosActivos(),
                    o.nochesDisponibles(), o.nochesVendidas(), o.porcentaje(), o.porApartamento().stream()
                    .map(a -> new OcupacionApartamentoR(a.codigoApartamento(), a.nochesVendidas(), a.porcentaje()))
                    .toList());
        }
    }

    public record IngresoMedioR(String medio, BigDecimal total, int pagos) {
    }

    public record IngresosR(LocalDate desde, LocalDate hasta, List<IngresoMedioR> porMedio, BigDecimal total,
                            int cantidadPagos) {
        static IngresosR de(Reportes.Ingresos i) {
            return new IngresosR(i.periodo().desde(), i.periodo().hasta(), i.porMedio().stream()
                    .map(m -> new IngresoMedioR(m.medio(), pesos(m.total()), m.pagos())).toList(),
                    pesos(i.total()), i.cantidadPagos());
        }
    }

    public record CanalR(Canal canal, Map<EstadoReserva, Long> porEstado, long total) {
    }

    public record CanalesR(LocalDate desde, LocalDate hasta, List<CanalR> canales, long total) {
        static CanalesR de(Reportes.ReservasPorCanal r) {
            return new CanalesR(r.periodo().desde(), r.periodo().hasta(), r.canales().stream()
                    .map(c -> new CanalR(c.canal(), c.porEstado(), c.total())).toList(), r.total());
        }
    }

    public record IndicadoresR(LocalDate desde, LocalDate hasta, BigDecimal ingresoAlojamiento,
                               BigDecimal tarifaPromedioNoche, BigDecimal ingresoPorNocheDisponible,
                               BigDecimal tasaNoShow, BigDecimal tasaCancelacion, BigDecimal antelacionPromedioDias) {
        static IndicadoresR de(Reportes.Indicadores i) {
            return new IndicadoresR(i.periodo().desde(), i.periodo().hasta(), pesos(i.ingresoAlojamiento()),
                    pesos(i.tarifaPromedioNoche()), pesos(i.ingresoPorNocheDisponible()), i.tasaNoShow(),
                    i.tasaCancelacion(), i.antelacionPromedioDias());
        }
    }

    private final ReporteServicio reportes;

    public ReporteControlador(ReporteServicio reportes) {
        this.reportes = reportes;
    }

    @GetMapping("/ocupacion")
    public OcupacionR ocupacion(@RequestParam LocalDate desde, @RequestParam LocalDate hasta) {
        return OcupacionR.de(reportes.ocupacion(desde, hasta));
    }

    @GetMapping("/ingresos")
    public IngresosR ingresos(@RequestParam LocalDate desde, @RequestParam LocalDate hasta) {
        return IngresosR.de(reportes.ingresos(desde, hasta));
    }

    @GetMapping("/canales")
    public CanalesR canales(@RequestParam LocalDate desde, @RequestParam LocalDate hasta) {
        return CanalesR.de(reportes.porCanal(desde, hasta));
    }

    @GetMapping("/indicadores")
    public IndicadoresR indicadores(@RequestParam LocalDate desde, @RequestParam LocalDate hasta) {
        return IndicadoresR.de(reportes.indicadores(desde, hasta));
    }

    @GetMapping("/{tipo}/csv")
    public ResponseEntity<?> csv(@PathVariable String tipo, @RequestParam LocalDate desde,
                                 @RequestParam LocalDate hasta) {
        List<List<Object>> filas = switch (tipo) {
            case "ocupacion" -> ocupacionCsv(reportes.ocupacion(desde, hasta));
            case "ingresos" -> ingresosCsv(reportes.ingresos(desde, hasta));
            case "canales" -> canalesCsv(reportes.porCanal(desde, hasta));
            case "indicadores" -> indicadoresCsv(reportes.indicadores(desde, hasta));
            default -> null;
        };
        if (filas == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorRespuesta.de("RECURSO_INEXISTENTE", "El reporte no existe", List.of()));
        }
        String nombre = "reporte-" + tipo + "-" + desde + "-a-" + hasta + ".csv";
        // El BOM hace que Excel reconozca las tildes; el separador es punto y coma, como en la configuración regional de Colombia.
        byte[] contenido = ("﻿" + aCsv(filas)).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(contenido);
    }

    // --- CSV ---

    private static List<List<Object>> ocupacionCsv(Reportes.Ocupacion o) {
        List<List<Object>> filas = new ArrayList<>();
        filas.add(List.of("Apartamento", "Noches vendidas", "Noches del periodo", "Ocupación (%)"));
        o.porApartamento().forEach(a -> filas.add(List.of(a.codigoApartamento(), a.nochesVendidas(),
                o.periodo().cantidadNoches(), a.porcentaje())));
        filas.add(List.of("TOTAL", o.nochesVendidas(), o.nochesDisponibles(), o.porcentaje()));
        return filas;
    }

    private static List<List<Object>> ingresosCsv(Reportes.Ingresos i) {
        List<List<Object>> filas = new ArrayList<>();
        filas.add(List.of("Medio de pago", "Pagos", "Total (COP)"));
        i.porMedio().forEach(m -> filas.add(List.of(m.medio(), m.pagos(), pesos(m.total()))));
        filas.add(List.of("TOTAL", i.cantidadPagos(), pesos(i.total())));
        return filas;
    }

    private static List<List<Object>> canalesCsv(Reportes.ReservasPorCanal r) {
        List<List<Object>> filas = new ArrayList<>();
        List<Object> encabezado = new ArrayList<>(List.of("Canal"));
        Arrays.stream(EstadoReserva.values()).forEach(e -> encabezado.add(e.name()));
        encabezado.add("Total");
        filas.add(encabezado);
        r.canales().forEach(c -> {
            List<Object> fila = new ArrayList<>(List.of(c.canal().name()));
            Arrays.stream(EstadoReserva.values()).forEach(e -> fila.add(c.porEstado().get(e)));
            fila.add(c.total());
            filas.add(fila);
        });
        return filas;
    }

    private static List<List<Object>> indicadoresCsv(Reportes.Indicadores i) {
        return List.of(List.of("Indicador", "Valor"),
                List.of("Ingreso por alojamiento (COP)", pesos(i.ingresoAlojamiento())),
                List.of("Tarifa promedio por noche vendida (COP)", pesos(i.tarifaPromedioNoche())),
                List.of("Ingreso por noche disponible (COP)", pesos(i.ingresoPorNocheDisponible())),
                List.of("Tasa de no-show (%)", i.tasaNoShow()),
                List.of("Tasa de cancelación (%)", i.tasaCancelacion()),
                List.of("Antelación promedio (días)", i.antelacionPromedioDias()));
    }

    private static String aCsv(List<List<Object>> filas) {
        StringBuilder texto = new StringBuilder();
        for (List<Object> fila : filas) {
            texto.append(String.join(";", fila.stream().map(ReporteControlador::celda).toList())).append("\r\n");
        }
        return texto.toString();
    }

    private static String celda(Object valor) {
        String texto = valor instanceof BigDecimal numero ? numero.toPlainString().replace('.', ',') : String.valueOf(valor);
        return texto.contains(";") || texto.contains("\"") ? "\"" + texto.replace("\"", "\"\"") + "\"" : texto;
    }
}
