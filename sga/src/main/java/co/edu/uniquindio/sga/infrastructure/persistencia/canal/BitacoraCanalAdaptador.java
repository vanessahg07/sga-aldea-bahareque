package co.edu.uniquindio.sga.infrastructure.persistencia.canal;

import co.edu.uniquindio.sga.domain.canal.BitacoraCanalRepositorio;
import co.edu.uniquindio.sga.domain.canal.EventoCanal;
import co.edu.uniquindio.sga.domain.comun.Pagina;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA de la bitácora de canales. Cada evento se guarda en su propia transacción
 * para que quede registrado aunque la operación del canal se rechace y se revierta.
 */
@Repository
@Transactional
public class BitacoraCanalAdaptador implements BitacoraCanalRepositorio {

    private static final int LONGITUD_DETALLE = 1000;

    public interface Jpa extends JpaRepository<EventoCanalEntity, String> {

        Page<EventoCanalEntity> findByCodigoCanal(String codigoCanal, Pageable pagina);
    }

    private final Jpa jpa;

    public BitacoraCanalAdaptador(Jpa jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(EventoCanal evento) {
        EventoCanalEntity e = new EventoCanalEntity();
        e.setId(evento.id());
        e.setCodigoCanal(recortar(evento.codigoCanal(), 255));
        e.setTipo(evento.tipo());
        e.setIdExterno(recortar(evento.idExterno(), 255));
        e.setIdReserva(evento.idReserva());
        e.setDetalle(recortar(evento.detalle(), LONGITUD_DETALLE));
        e.setFecha(evento.fecha());
        jpa.save(e);
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<EventoCanal> listar(String codigoCanal, int pagina) {
        Pageable orden = PageRequest.of(Math.max(pagina, 0), Pagina.TAMANO, Sort.by(Sort.Direction.DESC, "fecha"));
        Page<EventoCanalEntity> resultado = codigoCanal == null ? jpa.findAll(orden)
                : jpa.findByCodigoCanal(codigoCanal, orden);
        return new Pagina<>(resultado.map(e -> new EventoCanal(e.getId(), e.getCodigoCanal(), e.getTipo(),
                e.getIdExterno(), e.getIdReserva(), e.getDetalle(), e.getFecha())).getContent(),
                resultado.getNumber(), resultado.getTotalElements());
    }

    /** Lo que envía el canal no se controla: se recorta para no perder el evento por su longitud. */
    private static String recortar(String texto, int maximo) {
        return texto == null || texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}
