package com.quotare.cotacoes.service;

import com.quotare.cotacoes.domain.Indicador;
import com.quotare.cotacoes.dto.IndicadorResponse;
import com.quotare.cotacoes.mapper.IndicadorMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class IndicadorService {

    @Inject
    IndicadorMapper mapper;

    public IndicadorResponse buscarPorId(Long id) {
        Indicador indicador = Indicador.<Indicador>findByIdOptional(id)
                .orElseThrow(NotFoundException::new);

        return mapper.toResponse(indicador);
    }
}
