package co.edu.uniquindio.sga.infrastructure.persistencia.tarifa;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Cada fila es una tarifa con su vigencia; nunca se actualiza ni se borra. */
@Entity
@Table(name = "tarifa", indexes = @Index(columnList = "codigoApartamento, temporada"))
@Getter
@Setter
@NoArgsConstructor
public class TarifaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String codigoApartamento;
    private String temporada;
    @Column(precision = 15, scale = 2)
    private BigDecimal valor;
    private LocalDateTime vigenteDesde;
}
