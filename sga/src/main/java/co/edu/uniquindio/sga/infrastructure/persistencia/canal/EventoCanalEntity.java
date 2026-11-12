package co.edu.uniquindio.sga.infrastructure.persistencia.canal;

import co.edu.uniquindio.sga.domain.canal.TipoEventoCanal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "evento_canal", indexes = {@Index(columnList = "fecha"), @Index(columnList = "codigoCanal, fecha")})
@Getter
@Setter
@NoArgsConstructor
public class EventoCanalEntity {

    @Id
    private String id;
    private String codigoCanal;
    @Enumerated(EnumType.STRING)
    private TipoEventoCanal tipo;
    private String idExterno;
    private String idReserva;
    @Column(length = 1000)
    private String detalle;
    private LocalDateTime fecha;
}
