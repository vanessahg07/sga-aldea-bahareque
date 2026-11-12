package co.edu.uniquindio.sga.infrastructure.persistencia.apartamento;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "bloqueo")
@Getter
@Setter
@NoArgsConstructor
public class BloqueoEntity {

    @Id
    private String id;
    private String codigoApartamento;
    private LocalDate inicio;
    private LocalDate fin;
    private String motivo;
    /** Levantar un bloqueo lo deja no vigente; nunca se borra. */
    private boolean vigente;
}
