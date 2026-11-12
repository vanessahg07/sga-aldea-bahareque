package co.edu.uniquindio.sga.infrastructure.persistencia.usuario;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "codigo_recuperacion", indexes = @Index(columnList = "correo"))
@Getter
@Setter
@NoArgsConstructor
public class CodigoRecuperacionEntity {

    @Id
    private String id;
    private String correo;
    private String codigoCifrado;
    private LocalDateTime expiraEn;
    private boolean usado;
}
