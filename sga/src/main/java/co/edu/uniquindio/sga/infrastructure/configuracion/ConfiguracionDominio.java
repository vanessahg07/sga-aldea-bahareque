package co.edu.uniquindio.sga.infrastructure.configuracion;

import co.edu.uniquindio.sga.domain.canal.ProcesadorReservasExternas;
import co.edu.uniquindio.sga.domain.comun.Reloj;
import co.edu.uniquindio.sga.domain.recordatorio.PlanificadorRecordatorios;
import co.edu.uniquindio.sga.domain.reporte.CalculadoraReportes;
import co.edu.uniquindio.sga.domain.reserva.CotizadorEstancia;
import co.edu.uniquindio.sga.domain.reserva.GestorReservas;
import co.edu.uniquindio.sga.domain.reserva.VerificadorDisponibilidad;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Registra como beans los servicios del dominio, que no conocen Spring, y el reloj
 * en la zona horaria del alojamiento.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(PropiedadesSga.class)
@EnableJpaRepositories(basePackages = "co.edu.uniquindio.sga.infrastructure.persistencia",
        considerNestedRepositories = true)
public class ConfiguracionDominio {

    @Bean
    public Reloj reloj(PropiedadesSga propiedades) {
        ZoneId zona = ZoneId.of(propiedades.zonaHoraria());
        return () -> LocalDateTime.now(zona);
    }

    @Bean
    public VerificadorDisponibilidad verificadorDisponibilidad() {
        return new VerificadorDisponibilidad();
    }

    @Bean
    public CotizadorEstancia cotizadorEstancia() {
        return new CotizadorEstancia();
    }

    @Bean
    public GestorReservas gestorReservas(VerificadorDisponibilidad verificador, CotizadorEstancia cotizador) {
        return new GestorReservas(verificador, cotizador);
    }

    @Bean
    public ProcesadorReservasExternas procesadorReservasExternas(GestorReservas gestor) {
        return new ProcesadorReservasExternas(gestor);
    }

    @Bean
    public CalculadoraReportes calculadoraReportes() {
        return new CalculadoraReportes();
    }

    @Bean
    public PlanificadorRecordatorios planificadorRecordatorios() {
        return new PlanificadorRecordatorios();
    }
}
