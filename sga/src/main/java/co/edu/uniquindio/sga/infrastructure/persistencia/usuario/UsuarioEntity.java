package co.edu.uniquindio.sga.infrastructure.persistencia.usuario;

import co.edu.uniquindio.sga.domain.usuario.Rol;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class UsuarioEntity {

    @Id
    private String id;
    private String nombre;
    @Column(unique = true, nullable = false)
    private String correo;
    private String contrasenaCifrada;
    private String telefono;
    private String documento;
    private LocalDate fechaNacimiento;
    @Column(length = 1000)
    private String fotoUrl;
    @Enumerated(EnumType.STRING)
    private Rol rol;
    /** Eliminación lógica: el usuario deja de poder ingresar, pero sus reservas se conservan. */
    private boolean eliminado;
}
