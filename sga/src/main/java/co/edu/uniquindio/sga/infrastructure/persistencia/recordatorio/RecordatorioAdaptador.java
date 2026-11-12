package co.edu.uniquindio.sga.infrastructure.persistencia.recordatorio;

import co.edu.uniquindio.sga.domain.comun.Pagina;
import co.edu.uniquindio.sga.domain.recordatorio.Recordatorio;
import co.edu.uniquindio.sga.domain.recordatorio.RecordatorioRepositorio;
import co.edu.uniquindio.sga.domain.recordatorio.TipoRecordatorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Adaptador JPA del puerto {@link RecordatorioRepositorio}. */
@Repository
@Transactional
public class RecordatorioAdaptador implements RecordatorioRepositorio {

    public interface Jpa extends JpaRepository<RecordatorioEntity, String> {

        boolean existsByIdReservaAndTipo(String idReserva, TipoRecordatorio tipo);
    }

    private final Jpa jpa;

    public RecordatorioAdaptador(Jpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public void guardar(Recordatorio r) {
        RecordatorioEntity e = new RecordatorioEntity();
        e.setId(r.id());
        e.setIdReserva(r.idReserva());
        e.setTipo(r.tipo());
        e.setDestinatario(r.destinatario());
        e.setAsunto(r.asunto());
        e.setEnviadoEn(r.enviadoEn());
        jpa.save(e);
    }

    @Override
    public boolean fueEnviado(String idReserva, TipoRecordatorio tipo) {
        return jpa.existsByIdReservaAndTipo(idReserva, tipo);
    }

    @Override
    public Pagina<Recordatorio> listar(int pagina) {
        Page<RecordatorioEntity> resultado = jpa.findAll(PageRequest.of(Math.max(pagina, 0), Pagina.TAMANO,
                Sort.by(Sort.Direction.DESC, "enviadoEn")));
        return new Pagina<>(resultado.map(e -> new Recordatorio(e.getId(), e.getIdReserva(), e.getTipo(),
                e.getDestinatario(), e.getAsunto(), e.getEnviadoEn())).getContent(), resultado.getNumber(),
                resultado.getTotalElements());
    }
}
