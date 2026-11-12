package co.edu.uniquindio.sga.domain.comun;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Puerto que entrega la fecha y hora actuales en la zona horaria del alojamiento
 * (Colombia). Permite que el dominio no dependa del reloj del sistema.
 */
public interface Reloj {

    LocalDateTime ahora();

    default LocalDate hoy() {
        return ahora().toLocalDate();
    }
}
