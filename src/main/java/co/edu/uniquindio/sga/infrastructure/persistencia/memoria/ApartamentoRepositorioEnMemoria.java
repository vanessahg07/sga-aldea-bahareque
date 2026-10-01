package co.edu.uniquindio.sga.infrastructure.persistencia.memoria;

import co.edu.uniquindio.sga.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga.domain.apartamento.ApartamentoRepositorio;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementación en memoria del puerto {@link ApartamentoRepositorio}, indexada por el
 * código del apartamento. Los listados ignoran los apartamentos eliminados (eliminación lógica).
 */
public class ApartamentoRepositorioEnMemoria implements ApartamentoRepositorio {

    private final Map<String, Apartamento> apartamentos = new HashMap<>();

    @Override
    public void guardar(Apartamento apartamento) {
        apartamentos.put(apartamento.getCodigo(), apartamento);
    }

    @Override
    public Optional<Apartamento> buscar(String codigo) {
        return Optional.ofNullable(apartamentos.get(codigo));
    }

    @Override
    public boolean existe(String codigo) {
        return apartamentos.containsKey(codigo);
    }

    @Override
    public List<Apartamento> listarNoEliminados() {
        return apartamentos.values().stream()
                .filter(a -> !a.isEliminado())
                .sorted(Comparator.comparing(Apartamento::getCodigo))
                .toList();
    }
}
