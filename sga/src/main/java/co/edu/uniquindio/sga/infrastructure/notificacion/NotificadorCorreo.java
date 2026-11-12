package co.edu.uniquindio.sga.infrastructure.notificacion;

import co.edu.uniquindio.sga.domain.comun.Notificador;
import co.edu.uniquindio.sga.infrastructure.configuracion.PropiedadesSga;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Adaptador SMTP del puerto {@link Notificador} ({@code sga.notificaciones.proveedor=smtp}).
 * Sirve con cualquier servidor SMTP gratuito: una cuenta con clave de aplicación o un
 * buzón de pruebas como Mailtrap o Ethereal.
 * <p>
 * El envío ocurre en segundo plano y con tiempo de espera máximo: la operación de negocio
 * que origina el aviso (reservar, cancelar, recuperar la contraseña) nunca espera al
 * servidor de correo ni falla por él. Un envío fallido queda en el log (I-04, I-05).
 */
@Component
@ConditionalOnProperty(name = "sga.notificaciones.proveedor", havingValue = "smtp")
public class NotificadorCorreo implements Notificador {

    private static final Logger LOG = LoggerFactory.getLogger(NotificadorCorreo.class);

    private final JavaMailSenderImpl servidor;
    private final String remitente;
    private final ExecutorService envios = Executors.newVirtualThreadPerTaskExecutor();

    public NotificadorCorreo(PropiedadesSga propiedades) {
        PropiedadesSga.Notificaciones config = propiedades.notificaciones();
        PropiedadesSga.Smtp smtp = config.smtp();
        if (smtp.host() == null || smtp.host().isBlank()) {
            throw new IllegalStateException("sga.notificaciones.proveedor=smtp requiere el servidor en "
                    + "SGA_SMTP_HOST (y el usuario y la clave en SGA_SMTP_USUARIO y SGA_SMTP_CLAVE)");
        }
        servidor = new JavaMailSenderImpl();
        servidor.setHost(smtp.host());
        servidor.setPort(smtp.puerto());
        servidor.setUsername(smtp.usuario());
        servidor.setPassword(smtp.clave());
        servidor.setDefaultEncoding("UTF-8");
        Properties opciones = servidor.getJavaMailProperties();
        String espera = String.valueOf(config.timeoutMs());
        opciones.put("mail.smtp.auth", String.valueOf(smtp.usuario() != null && !smtp.usuario().isBlank()));
        opciones.put("mail.smtp.starttls.enable", String.valueOf(smtp.starttls()));
        opciones.put("mail.smtp.connectiontimeout", espera);
        opciones.put("mail.smtp.timeout", espera);
        opciones.put("mail.smtp.writetimeout", espera);
        remitente = config.remitente() == null || config.remitente().isBlank() ? smtp.usuario() : config.remitente();
        LOG.info("Notificaciones por correo con el servidor {}:{} (espera máxima {} ms)", smtp.host(), smtp.puerto(),
                config.timeoutMs());
    }

    @Override
    public void notificar(Notificacion notificacion) {
        envios.submit(() -> enviar(notificacion));
    }

    private void enviar(Notificacion notificacion) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(notificacion.destinatario());
        mensaje.setSubject(notificacion.asunto());
        mensaje.setText(notificacion.mensaje());
        try {
            servidor.send(mensaje);
            LOG.info("Correo enviado a {}: {}", notificacion.destinatario(), notificacion.asunto());
        } catch (MailException e) {
            LOG.warn("No se pudo enviar el correo a {} ({}): {}", notificacion.destinatario(), notificacion.asunto(),
                    e.getMessage());
        }
    }

    @PreDestroy
    public void detener() {
        envios.close();
    }
}
