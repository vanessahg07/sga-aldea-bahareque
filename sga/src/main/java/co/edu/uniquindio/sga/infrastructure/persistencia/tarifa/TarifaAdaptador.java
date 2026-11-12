package co.edu.uniquindio.sga.infrastructure.persistencia.tarifa;

import co.edu.uniquindio.sga.domain.comun.Dinero;
import co.edu.uniquindio.sga.domain.tarifa.TablaTarifas;
import co.edu.uniquindio.sga.domain.tarifa.Tarifa;
import co.edu.uniquindio.sga.domain.tarifa.TarifaRepositorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Adaptador JPA del puerto {@link TarifaRepositorio}. */
@Repository
@Transactional
public class TarifaAdaptador implements TarifaRepositorio {

    public interface Jpa extends JpaRepository<TarifaEntity, Long> {
    }

    private final Jpa jpa;

    public TarifaAdaptador(Jpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public TablaTarifas tabla() {
        return new TablaTarifas(jpa.findAll().stream()
                .map(e -> new Tarifa(e.getCodigoApartamento(), e.getTemporada(), Dinero.de(e.getValor()),
                        e.getVigenteDesde()))
                .toList());
    }

    @Override
    public void registrar(Tarifa tarifa) {
        TarifaEntity e = new TarifaEntity();
        e.setCodigoApartamento(tarifa.codigoApartamento());
        e.setTemporada(tarifa.nombreTemporada());
        e.setValor(tarifa.valor().valor());
        e.setVigenteDesde(tarifa.vigenteDesde());
        jpa.save(e);
    }
}
