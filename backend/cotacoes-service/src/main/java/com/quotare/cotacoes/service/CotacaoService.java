package com.quotare.cotacoes.service;

import com.quotare.cotacoes.domain.Cotacao;
import com.quotare.cotacoes.domain.FonteDados;
import com.quotare.cotacoes.domain.Indicador;
import com.quotare.cotacoes.dto.AtualizarCotacaoRequest;
import com.quotare.cotacoes.dto.CriarCotacaoRequest;
import com.quotare.cotacoes.dto.CotacaoResponse;
import com.quotare.cotacoes.dto.PaginaResponse;
import com.quotare.cotacoes.exception.ConflitoDeConcorrenciaException;
import com.quotare.cotacoes.exception.CotacaoFonteExclusivaException;
import com.quotare.cotacoes.mapper.CotacaoMapper;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import org.hibernate.exception.ConstraintViolationException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class CotacaoService {

    @Inject
    CotacaoMapper mapper;

    @Transactional
    public CotacaoResponse criar(CriarCotacaoRequest request) {
        Indicador indicador = Indicador.<Indicador>findByIdOptional(request.indicadorId())
                .orElseThrow(NotFoundException::new);
        garantirFonteLocal(indicador);

        Cotacao cotacao = mapper.toEntity(request);
        cotacao.indicador = indicador;
        cotacao.fonte = FonteDados.LOCAL;

        try {
            cotacao.persistAndFlush();
        } catch (ConstraintViolationException exception) {
            throw new ConflitoDeConcorrenciaException();
        }

        Cotacao.getEntityManager().refresh(cotacao);

        return mapper.toResponse(cotacao);
    }

    @Transactional
    public CotacaoResponse atualizar(Long id, AtualizarCotacaoRequest request) {
        Cotacao cotacao = Cotacao.<Cotacao>findByIdOptional(id).orElseThrow(NotFoundException::new);
        garantirFonteLocal(cotacao.indicador);

        Indicador indicador = Indicador.<Indicador>findByIdOptional(request.indicadorId())
                .orElseThrow(NotFoundException::new);
        garantirFonteLocal(indicador);

        mapper.atualizar(request, cotacao);
        cotacao.indicador = indicador;

        try {
            Cotacao.getEntityManager().flush();
        } catch (ConstraintViolationException exception) {
            throw new ConflitoDeConcorrenciaException();
        }

        return mapper.toResponse(cotacao);
    }

    @Transactional
    public void remover(Long id) {
        Cotacao cotacao = Cotacao.<Cotacao>findByIdOptional(id).orElseThrow(NotFoundException::new);
        garantirFonteLocal(cotacao.indicador);
        cotacao.delete();
    }

    private void garantirFonteLocal(Indicador indicador) {
        if (indicador.fonte == FonteDados.EXTERNA) {
            throw new CotacaoFonteExclusivaException(indicador.codigo);
        }
    }

    public CotacaoResponse buscarPorId(Long id) {
        Cotacao cotacao = Cotacao.<Cotacao>findByIdOptional(id)
                .orElseThrow(NotFoundException::new);

        return mapper.toResponse(cotacao);
    }

    public PaginaResponse<CotacaoResponse> listar(int page, int size, Long indicadorId, Instant inicio, Instant fim) {
        var sort = Sort.by("id");

        List<String> condicoes = new ArrayList<>();
        Map<String, Object> parametros = new HashMap<>();

        if (indicadorId != null) {
            condicoes.add("indicador.id = :indicadorId");
            parametros.put("indicadorId", indicadorId);
        }
        if (inicio != null) {
            condicoes.add("dataHora >= :inicio");
            parametros.put("inicio", inicio);
        }
        if (fim != null) {
            condicoes.add("dataHora <= :fim");
            parametros.put("fim", fim);
        }

        var query = condicoes.isEmpty()
                ? Cotacao.findAll(sort)
                : Cotacao.find(String.join(" and ", condicoes), sort, parametros);

        var paginado = query.page(Page.of(page, size));

        var itens = paginado.<Cotacao>list().stream()
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
