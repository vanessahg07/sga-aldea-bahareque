package co.edu.uniquindio.sga.infrastructure.persistencia.canal;

import co.edu.uniquindio.sga.domain.comun.CodigoError;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "conflicto_canal", indexes = @Index(columnList = "registradoEn"))
@Getter
@Setter
@NoArgsConstructor
public class ConflictoCanalEntity {

    @Id
    private String id;
    private String canal;
    private String idExterno;
    private String codigoApartamento;
    private LocalDate entrada;
    private LocalDate salida;
    private String titular;
    @Enumerated(EnumType.STRING)
    private CodigoError motivo;
    private LocalDateTime registradoEn;
    private boolean revisado;
    private String revisadoPor;
    @Column(length = 1000)
    private String notaRevision;
    private LocalDateTime fechaRevision;
}
