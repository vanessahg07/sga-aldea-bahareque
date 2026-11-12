package co.edu.uniquindio.sga.infrastructure.persistencia.apartamento;

import co.edu.uniquindio.sga.domain.apartamento.EstadoOperativo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "apartamento")
@Getter
@Setter
@NoArgsConstructor
public class ApartamentoEntity {

    @Id
    private String codigo;
    private String nombre;
    @Column(length = 2000)
    private String descripcion;
    private int dormitorios;
    private int capacidad;
    private boolean admiteMascotas;
    private boolean activo;
    private boolean eliminado;
    @Enumerated(EnumType.STRING)
    private EstadoOperativo estadoOperativo;

    @ElementCollection
    @CollectionTable(name = "apartamento_caracteristica", joinColumns = @JoinColumn(name = "codigo_apartamento"))
    @Column(name = "caracteristica")
    private Set<String> caracteristicas = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "apartamento_imagen", joinColumns = @JoinColumn(name = "codigo_apartamento"))
    @OrderColumn(name = "orden")
    private List<Imagen> imagenes = new ArrayList<>();

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Imagen {
        @Column(length = 1000)
        private String url;
        private boolean principal;
    }
}
