package co.edu.uniquindio.sga.infrastructure.persistencia.recordatorio;

import co.edu.uniquindio.sga.domain.recordatorio.TipoRecordatorio;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Un recordatorio por reserva y tipo: la restricción única impide enviarlo dos veces. */
@Entity
@Table(name = "recordatorio",
        uniqueConstraints = @UniqueConstraint(name = "uk_recordatorio_reserva_tipo", columnNames = {"idReserva", "tipo"}),
        indexes = @Index(columnList = "enviadoEn"))
@Getter
@Setter
@NoArgsConstructor
public class RecordatorioEntity {

    @Id
    private String id;
    private String idReserva;
    @Enumerated(EnumType.STRING)
    private TipoRecordatorio tipo;
    private String destinatario;
    private String asunto;
    private LocalDateTime enviadoEn;
}
