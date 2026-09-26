package co.edu.uniquindio.sga.domain.comun;

/**
 * Puerto de salida para notificar a una persona (correo o mensajería). Una falla al
 * notificar nunca debe impedir la operación de negocio que la origina.
 */
public interface Notificador {

    void notificar(Notificacion notificacion);

    record Notificacion(String destinatario, String asunto, String mensaje) {
    }
}
