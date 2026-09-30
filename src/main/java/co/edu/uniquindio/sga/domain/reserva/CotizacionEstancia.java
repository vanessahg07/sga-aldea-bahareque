package co.edu.uniquindio.sga.domain.reserva;

import co.edu.uniquindio.sga.domain.comun.Dinero;

import java.util.List;

/**
 * Resultado de cotizar una estancia. Al crear la reserva se congela tal cual.
 *
 * @param detalle            desglose noche por noche
 * @param subtotalNoches     suma de los subtotales por noche, sin redondear
 * @param descuento          descuento por estancia larga (RP-02), sin redondear
 * @param valorAlojamiento   cargo de alojamiento: subtotal − descuento, redondeado al final
 * @param valorMascotas      cargo por mascotas (RP-03), redondeado
 */
public record CotizacionEstancia(List<DetalleNoche> detalle, Dinero subtotalNoches, Dinero descuento,
                                 Dinero valorAlojamiento, Dinero valorMascotas) {

    public CotizacionEstancia {
        detalle = List.copyOf(detalle);
    }

    public Dinero valorTotal() {
        return valorAlojamiento.mas(valorMascotas);
    }
}
