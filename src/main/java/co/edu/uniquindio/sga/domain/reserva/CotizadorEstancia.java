package co.edu.uniquindio.sga.domain.reserva;

import co.edu.uniquindio.sga.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga.domain.comun.Dinero;
import co.edu.uniquindio.sga.domain.tarifa.Temporada;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcula el valor de una estancia noche por noche (RN-05): para cada noche, la tarifa
 * vigente del apartamento en la temporada de esa noche por el número de ocupantes
 * facturables (RN-06). El redondeo se aplica al final de cada cargo, nunca por noche.
 */
public class CotizadorEstancia {

    public CotizacionEstancia cotizar(String codigoApartamento, Estancia estancia, List<Ocupante> ocupantes,
                                      int mascotas, ContextoReserva contexto) {
        ParametrosAlojamiento parametros = contexto.parametros();
        int facturables = (int) ocupantes.stream()
                .filter(o -> o.esFacturable(estancia.fechaEntrada(), parametros.umbralEdadFacturable()))
                .count();

        List<DetalleNoche> detalle = new ArrayList<>();
        Dinero subtotal = Dinero.CERO;
        for (LocalDate noche : estancia.listaNoches()) {
            Temporada temporada = contexto.calendario().temporadaDe(noche);
            Dinero tarifa = contexto.tarifas().valorPara(codigoApartamento, temporada.nombre());
            Dinero subtotalNoche = tarifa.por(facturables);
            detalle.add(new DetalleNoche(noche, temporada.nombre(), tarifa, facturables, subtotalNoche));
            subtotal = subtotal.mas(subtotalNoche);
        }

        // RP-02: descuento por estancia larga, sobre el cargo de alojamiento
        Dinero descuento = estancia.noches() >= parametros.nochesDescuentoLargo()
                ? subtotal.porcentaje(parametros.porcentajeDescuentoLargo())
                : Dinero.CERO;
        Dinero valorAlojamiento = subtotal.menos(descuento).redondear();

        // RP-03: cargo fijo por mascota, por estancia
        Dinero valorMascotas = parametros.cargoMascota().por(mascotas).redondear();

        return new CotizacionEstancia(detalle, subtotal, descuento, valorAlojamiento, valorMascotas);
    }
}
