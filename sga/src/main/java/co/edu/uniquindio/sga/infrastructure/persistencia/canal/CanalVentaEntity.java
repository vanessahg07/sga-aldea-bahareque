package co.edu.uniquindio.sga.infrastructure.persistencia.canal;

import co.edu.uniquindio.sga.domain.canal.TipoCanal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "canal_venta")
@Getter
@Setter
@NoArgsConstructor
public class CanalVentaEntity {

    @Id
    private String codigo;
    private String nombre;
    @Enumerated(EnumType.STRING)
    private TipoCanal tipo;
    private String credencialCifrada;
    private boolean activo;
}
