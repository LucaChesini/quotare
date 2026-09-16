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
import com.quotare.cotacoes.dto.ResumoResponse;
import com.quotare.cotacoes.dto.SerieResponse;
import com.quotare.cotacoes.exception.ConflitoDeConcorrenciaException;
import com.quotare.cotacoes.exception.CotacaoDuplicadaException;
import com.quotare.cotacoes.exception.CotacaoFonteExclusivaException;
import com.quotare.cotacoes.exception.IntervaloInvalidoException;
import com.quotare.cotacoes.mapper.CotacaoMapper;
import com.quotare.cotacoes.mapper.IndicadorMapper;
import com.quotare.cotacoes.provider.CotacaoDTO;
import com.quotare.cotacoes.provider.CotacaoProvider;
import com.quotare.cotacoes.provider.CotacaoProviderFactory;
import com.quotare.cotacoes.provider.CotacaoSerieRepository;
import com.quotare.cotacoes.provider.MinMaxCotacao;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.exception.ConstraintViolationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class CotacaoService {

    @Inject
    CotacaoMapper mapper;

    @Inject
    IndicadorMapper indicadorMapper;

    @Inject
    CotacaoProviderFactory providerFactory;

    @Inject
    CotacaoSerieRepository serieRepository;

    @ConfigProperty(name = "cotacoes.serie.intervalo-maximo-dias")
    long intervaloMaximoDias;

    private static final int LIMITE_PONTOS = 2000;

    @Transactional
    public CotacaoResponse criar(CriarCotacaoRequest request) {
        Indicador indicador = Indicador.<Indicador>findByIdOptional(request.indicadorId())
                .orElseThrow(NotFoundException::new);
        garantirFonteLocal(indicador);

        Cotacao cotacao = mapper.toEntity(request);
        cotacao.indicador = indicador;
        cotacao.fonte = FonteDados.LOCAL;
        garantirPontoDisponivel(indicador.id, cotacao.dataHora, cotacao.fonte, null);

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
        garantirPontoDisponivel(indicador.id, request.dataHora(), cotacao.fonte, id);

        mapper.atualizar(request, cotacao);
        cotacao.indicador = indicador;

        try {
            Cotacao.getEntityManager().flush();
        } catch (ConstraintViolationException exception) {
            throw new ConflitoDeConcorrenciaException();
        }

        Cotacao.getEntityManager().refresh(cotacao);

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

    private void garantirPontoDisponivel(Long indicadorId, Instant dataHora, FonteDados fonte, Long idIgnorado) {
        long existentes = idIgnorado == null
                ? Cotacao.count("indicador.id = ?1 and dataHora = ?2 and fonte = ?3", indicadorId, dataHora, fonte)
                : Cotacao.count("indicador.id = ?1 and dataHora = ?2 and fonte = ?3 and id <> ?4", indicadorId, dataHora, fonte, idIgnorado);

        if (existentes > 0) {
            Indicador indicador = Indicador.<Indicador>findByIdOptional(indicadorId).orElseThrow(NotFoundException::new);
            throw new CotacaoDuplicadaException(indicador.codigo, dataHora, fonte);
        }
    }

    public CotacaoResponse buscarPorId(Long id) {
        Cotacao cotacao = Cotacao.<Cotacao>findByIdOptional(id)
                .orElseThrow(NotFoundException::new);

        return mapper.toResponse(cotacao);
    }

    public PaginaResponse<CotacaoResponse> listar(int page, int size, Long indicadorId, Instant inicio, Instant fim) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new IntervaloInvalidoException("A data de início não pode ser posterior à data de fim");
        }

        var sort = Sort.by("c.id");

        List<String> condicoes = new ArrayList<>();
        Map<String, Object> parametros = new HashMap<>();

        if (indicadorId != null) {
            condicoes.add("c.indicador.id = :indicadorId");
            parametros.put("indicadorId", indicadorId);
        }
        if (inicio != null) {
            condicoes.add("c.dataHora >= :inicio");
            parametros.put("inicio", inicio);
        }
        if (fim != null) {
            condicoes.add("c.dataHora <= :fim");
            parametros.put("fim", fim);
        }

        String jpql = "FROM Cotacao c JOIN FETCH c.indicador";
        if (!condicoes.isEmpty()) {
            jpql += " WHERE " + String.join(" and ", condicoes);
        }

        var query = Cotacao.find(jpql, sort, parametros);

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

    public SerieResponse buscarSerie(Long indicadorId, Instant inicio, Instant fim, GranularidadeSerie granularidadeSolicitada) {
        if (!inicio.isBefore(fim)) {
            throw new IntervaloInvalidoException("A data de início deve ser anterior à data de fim");
        }

        long duracaoEmDias = ChronoUnit.DAYS.between(inicio, fim);
        if (duracaoEmDias > intervaloMaximoDias) {
            throw new IntervaloInvalidoException(
                    "Intervalo de " + duracaoEmDias + " dias excede o máximo permitido de " + intervaloMaximoDias + " dias"
            );
        }

        GranularidadeSerie granularidadeEfetiva = determinarGranularidadeEfetiva(indicadorId, inicio, fim, granularidadeSolicitada);

        Indicador indicador = Indicador.<Indicador>findByIdOptional(indicadorId).orElseThrow(NotFoundException::new);

        CotacaoProvider provider = providerFactory.para(indicador.fonte);
        List<CotacaoDTO> pontosDoProvider = provider.buscarSerie(indicador.codigo, inicio, fim, granularidadeEfetiva);
        List<PontoResponse> pontos = pontosDoProvider.stream()
                .map(ponto -> new PontoResponse(ponto.dataHora(), ponto.valor()))
                .toList();

        MinMaxCotacao minMax = serieRepository.buscarMinMax(indicadorId, inicio, fim);
        ResumoResponse resumo = calcularResumo(pontos, minMax);

        return new SerieResponse(indicadorMapper.toResumoResponse(indicador), granularidadeEfetiva, pontos, resumo);
    }

    private GranularidadeSerie determinarGranularidadeEfetiva(Long indicadorId, Instant inicio, Instant fim, GranularidadeSerie granularidadeSolicitada) {
        List<GranularidadeSerie> candidatas = escadaDeGranularidades(granularidadeSolicitada);

        for (int i = 0; i < candidatas.size(); i++) {
            GranularidadeSerie candidata = candidatas.get(i);
            boolean ultimaCandidata = i == candidatas.size() - 1;

            if (ultimaCandidata) {
                return candidata;
            }

            long quantidadeDePontos = contarPontos(indicadorId, inicio, fim, candidata);
            if (quantidadeDePontos <= LIMITE_PONTOS) {
                return candidata;
            }
        }

        return GranularidadeSerie.MES;
    }

    private List<GranularidadeSerie> escadaDeGranularidades(GranularidadeSerie granularidadeSolicitada) {
        return switch (granularidadeSolicitada) {
            case BRUTO -> List.of(GranularidadeSerie.BRUTO, GranularidadeSerie.DIA, GranularidadeSerie.SEMANA, GranularidadeSerie.MES);
            case HORA -> List.of(GranularidadeSerie.HORA, GranularidadeSerie.DIA, GranularidadeSerie.SEMANA, GranularidadeSerie.MES);
            case DIA -> List.of(GranularidadeSerie.DIA, GranularidadeSerie.SEMANA, GranularidadeSerie.MES);
            case SEMANA -> List.of(GranularidadeSerie.SEMANA, GranularidadeSerie.MES);
            case MES -> List.of(GranularidadeSerie.MES);
        };
    }

    private long contarPontos(Long indicadorId, Instant inicio, Instant fim, GranularidadeSerie granularidade) {
        if (granularidade == GranularidadeSerie.BRUTO) {
            return Cotacao.count(
                    "indicador.id = :indicadorId and dataHora >= :inicio and dataHora <= :fim",
                    Map.of("indicadorId", indicadorId, "inicio", inicio, "fim", fim)
            );
        }

        String expressaoTruncamento = CotacaoSerieRepository.expressaoTruncamento(granularidade);

        String sql = "SELECT COUNT(DISTINCT " + expressaoTruncamento + ") "
                + "FROM cotacao "
                + "WHERE indicador_id = :indicadorId AND data_hora BETWEEN :inicio AND :fim";

        Object resultado = Cotacao.getEntityManager()
                .createNativeQuery(sql)
                .setParameter("indicadorId", indicadorId)
                .setParameter("inicio", inicio)
                .setParameter("fim", fim)
                .setParameter("fuso", serieRepository.fuso().getId())
                .getSingleResult();

        return ((Number) resultado).longValue();
    }

    ResumoResponse calcularResumo(List<PontoResponse> pontos, MinMaxCotacao minMax) {
        if (pontos.isEmpty()) {
            return new ResumoResponse(null, null, null);
        }

        BigDecimal minimo = minMax.minimo();
        BigDecimal maximo = minMax.maximo();

        BigDecimal primeiro = pontos.get(0).v();
        BigDecimal ultimo = pontos.get(pontos.size() - 1).v();

        BigDecimal variacaoPercentual = null;
        if (primeiro.compareTo(BigDecimal.ZERO) != 0) {
            variacaoPercentual = ultimo.subtract(primeiro)
                    .divide(primeiro, 10, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return new ResumoResponse(minimo, maximo, variacaoPercentual);
    }
}
