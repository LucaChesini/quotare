package com.quotare.cotacoes.service;

import com.quotare.cotacoes.domain.FonteDados;
import com.quotare.cotacoes.domain.Indicador;
import com.quotare.cotacoes.dto.IndicadorResponse;
import com.quotare.cotacoes.dto.PaginaResponse;
import com.quotare.cotacoes.mapper.IndicadorMapper;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class IndicadorService {

    @Inject
    IndicadorMapper mapper;

    public IndicadorResponse buscarPorId(Long id) {
        Indicador indicador = Indicador.<Indicador>findByIdOptional(id)
                .orElseThrow(NotFoundException::new);

        return mapper.toResponse(indicador);
    }

    public PaginaResponse<IndicadorResponse> listar(int page, int size, FonteDados fonte, Boolean ativo) {
        var sort = Sort.by("id");

        List<String> condicoes = new ArrayList<>();
        Map<String, Object> parametros = new HashMap<>();

        if (fonte != null) {
            condicoes.add("fonte = :fonte");
            parametros.put("fonte", fonte);
        }
        if (ativo != null) {
            condicoes.add("ativo = :ativo");
            parametros.put("ativo", ativo);
        }

        var query = condicoes.isEmpty()
                ? Indicador.findAll(sort)
                : Indicador.find(String.join(" and ", condicoes), sort, parametros);

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
