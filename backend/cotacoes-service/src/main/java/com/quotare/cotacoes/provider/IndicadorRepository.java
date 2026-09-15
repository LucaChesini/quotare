package com.quotare.cotacoes.provider;

import com.quotare.cotacoes.domain.FonteDados;
import com.quotare.cotacoes.domain.Indicador;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class IndicadorRepository {

    public List<Indicador> listarExternosAtivos() {
        return Indicador.list("fonte = ?1 and ativo = true", FonteDados.EXTERNA);
    }
}
