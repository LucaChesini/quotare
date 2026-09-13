package com.quotare.cotacoes.service;

import com.quotare.cotacoes.domain.Cotacao;
import com.quotare.cotacoes.domain.FonteDados;
import com.quotare.cotacoes.domain.Indicador;
import com.quotare.cotacoes.dto.AtualizarCotacaoRequest;
import com.quotare.cotacoes.dto.CriarCotacaoRequest;
import com.quotare.cotacoes.dto.CotacaoResponse;
import com.quotare.cotacoes.dto.GranularidadeSerie;
import com.quotare.cotacoes.dto.PaginaResponse;
import com.quotare.cotacoes.dto.PontoResponse;
import com.quotare.cotacoes.exception.ConflitoDeConcorrenciaException;
import com.quotare.cotacoes.exception.CotacaoFonteExclusivaException;
import com.quotare.cotacoes.mapper.CotacaoMapper;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.Tuple;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import org.hibernate.exception.ConstraintViolationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    public List<PontoResponse> buscarPontos(Long indicadorId, Instant inicio, Instant fim, GranularidadeSerie granularidade) {
        if (granularidade == GranularidadeSerie.BRUTO) {
            return buscarPontosBruto(indicadorId, inicio, fim);
        }

        return buscarPontosAgregados(indicadorId, inicio, fim, granularidade);
    }

    private List<PontoResponse> buscarPontosBruto(Long indicadorId, Instant inicio, Instant fim) {
        List<Cotacao> cotacoes = Cotacao.find(
                "indicador.id = :indicadorId and dataHora >= :inicio and dataHora <= :fim",
                Sort.by("dataHora"),
                Map.of("indicadorId", indicadorId, "inicio", inicio, "fim", fim)
        ).list();

        return cotacoes.stream()
                .map(cotacao -> new PontoResponse(cotacao.dataHora, cotacao.valor))
                .toList();
    }

    private List<PontoResponse> buscarPontosAgregados(Long indicadorId, Instant inicio, Instant fim, GranularidadeSerie granularidade) {
        String expressaoTruncamento = expressaoTruncamento(granularidade);

        String sql = "SELECT UNIX_TIMESTAMP(" + expressaoTruncamento + ") AS timestamp_em_segundos, AVG(valor) AS v "
                + "FROM cotacao "
                + "WHERE indicador_id = :indicadorId AND data_hora BETWEEN :inicio AND :fim "
                + "GROUP BY UNIX_TIMESTAMP(" + expressaoTruncamento + ") "
                + "ORDER BY timestamp_em_segundos";

        @SuppressWarnings("unchecked")
        List<Tuple> linhas = Cotacao.getEntityManager()
                .createNativeQuery(sql, Tuple.class)
                .setParameter("indicadorId", indicadorId)
                .setParameter("inicio", inicio)
                .setParameter("fim", fim)
                .getResultList();

        return linhas.stream()
                .map(linha -> {
                    long timestampEmSegundos = linha.get("timestamp_em_segundos", Number.class).longValue();
                    Instant t = Instant.ofEpochSecond(timestampEmSegundos);

                    BigDecimal v = linha.get("v", BigDecimal.class).setScale(6, RoundingMode.HALF_UP);

                    return new PontoResponse(t, v);
                })
                .toList();
    }

    private String expressaoTruncamento(GranularidadeSerie granularidade) {
        return switch (granularidade) {
            case HORA -> "DATE_ADD(DATE(data_hora), INTERVAL HOUR(data_hora) HOUR)";
            case DIA -> "DATE(data_hora)";
            case SEMANA -> "DATE_SUB(DATE(data_hora), INTERVAL WEEKDAY(data_hora) DAY)";
            case MES -> "DATE_SUB(DATE(data_hora), INTERVAL DAYOFMONTH(data_hora)-1 DAY)";
            case BRUTO -> throw new IllegalArgumentException("BRUTO não usa agregação");
        };
    }
}
