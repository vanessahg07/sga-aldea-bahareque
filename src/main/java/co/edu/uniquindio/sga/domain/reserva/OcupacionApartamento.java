package co.edu.uniquindio.sga.domain.reserva;

import co.edu.uniquindio.sga.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga.domain.apartamento.Bloqueo;

import java.util.List;

/** Apartamento junto con sus reservas y bloqueos, tal como los entrega la persistencia. */
public record OcupacionApartamento(Apartamento apartamento, List<Reserva> reservas, List<Bloqueo> bloqueos) {

    public OcupacionApartamento {
        reservas = List.copyOf(reservas);
        bloqueos = List.copyOf(bloqueos);
    }
}
