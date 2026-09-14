package com.quotare.cotacoes.provider;

import com.quotare.cotacoes.domain.Cotacao;
import com.quotare.cotacoes.domain.FonteDados;
import com.quotare.cotacoes.domain.Indicador;
import com.quotare.cotacoes.dto.GranularidadeSerie;
import com.quotare.cotacoes.dto.PontoResponse;

import io.quarkus.panache.common.Sort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Tuple;
import jakarta.ws.rs.NotFoundException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class LocalCotacaoProvider implements CotacaoProvider {

    @Override
    public List<CotacaoDTO> buscarSerie(String codigoIndicador, Instant inicio, Instant fim, GranularidadeSerie granularidade) {
        Indicador indicador = resolverIndicadorPorCodigo(codigoIndicador);

        List<PontoResponse> pontos = buscarPontos(indicador.id, inicio, fim, granularidade);

        return pontos.stream()
                .map(ponto -> new CotacaoDTO(ponto.v(), ponto.t()))
                .toList();
    }

    @Override
    public Optional<CotacaoDTO> buscarUltima(String codigoIndicador) {
        Indicador indicador = resolverIndicadorPorCodigo(codigoIndicador);

        return Cotacao.<Cotacao>find("indicador.id = ?1", Sort.by("dataHora").descending(), indicador.id)
                .firstResultOptional()
                .map(cotacao -> new CotacaoDTO(cotacao.valor, cotacao.dataHora));
    }

    @Override
    public List<IndicadorDTO> listarIndicadoresDisponiveis() {
        return Indicador.<Indicador>find("fonte = ?1 and ativo = true", FonteDados.LOCAL)
                .list()
                .stream()
                .map(indicador -> new IndicadorDTO(indicador.codigo, indicador.nome))
                .toList();
    }

    @Override
    public boolean suporta(FonteDados fonte) {
        return fonte == FonteDados.LOCAL;
    }

    private Indicador resolverIndicadorPorCodigo(String codigoIndicador) {
        String codigoNormalizado = codigoIndicador.toUpperCase();

        return Indicador.<Indicador>find("codigo", codigoNormalizado)
                .firstResultOptional()
                .orElseThrow(NotFoundException::new);
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

    public static String expressaoTruncamento(GranularidadeSerie granularidade) {
        return switch (granularidade) {
            case HORA -> "DATE_ADD(DATE(data_hora), INTERVAL HOUR(data_hora) HOUR)";
            case DIA -> "DATE(data_hora)";
            case SEMANA -> "DATE_SUB(DATE(data_hora), INTERVAL WEEKDAY(data_hora) DAY)";
            case MES -> "DATE_SUB(DATE(data_hora), INTERVAL DAYOFMONTH(data_hora)-1 DAY)";
            case BRUTO -> throw new IllegalArgumentException("BRUTO não usa agregação");
        };
    }
}
