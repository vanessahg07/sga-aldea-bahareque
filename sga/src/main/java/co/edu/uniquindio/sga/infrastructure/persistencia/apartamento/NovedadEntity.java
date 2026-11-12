package co.edu.uniquindio.sga.infrastructure.persistencia.apartamento;

import co.edu.uniquindio.sga.domain.apartamento.Novedad;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "novedad")
@Getter
@Setter
@NoArgsConstructor
public class NovedadEntity {

    @Id
    private String id;
    private String codigoApartamento;
    private LocalDateTime fecha;
    private String autor;
    @Column(length = 2000)
    private String descripcion;
    @Enumerated(EnumType.STRING)
    private Novedad.Gravedad gravedad;
}
