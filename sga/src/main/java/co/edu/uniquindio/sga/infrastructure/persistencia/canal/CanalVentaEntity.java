package co.edu.uniquindio.sga.infrastructure.persistencia.canal;

import co.edu.uniquindio.sga.domain.canal.TipoCanal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Tabla de canales de venta externos con el hash de su credencial de acceso. */
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
