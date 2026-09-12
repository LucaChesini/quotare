package com.quotare.cotacoes.service;

import com.quotare.cotacoes.domain.Indicador;
import com.quotare.cotacoes.dto.IndicadorResponse;
import com.quotare.cotacoes.dto.PaginaResponse;
import com.quotare.cotacoes.mapper.IndicadorMapper;

import io.quarkus.panache.common.Page;

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

    public PaginaResponse<IndicadorResponse> listar(int page, int size) {
        var query = Indicador.findAll();
        var paginado = query.page(Page.of(page, size));

        var itens = paginado.<Indicador>list().stream()
                .map(mapper::toResponse)
                .toList();

        return new PaginaResponse<>(
                itens,
                page,
                size,
                paginado.count(),
                paginado.pageCount()
        );
    }
}
