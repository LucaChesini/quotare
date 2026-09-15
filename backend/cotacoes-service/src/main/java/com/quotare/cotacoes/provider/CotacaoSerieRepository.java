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
public class CotacaoSerieRepository {

    public Indicador buscarIndicadorPorCodigo(String codigoIndicador) {
        String codigoNormalizado = codigoIndicador.toUpperCase();

        return Indicador.<Indicador>find("codigo", codigoNormalizado)
                .firstResultOptional()
                .orElseThrow(NotFoundException::new);
    }

    public Optional<Cotacao> buscarUltimaCotacao(Long indicadorId) {
        return Cotacao.<Cotacao>find("indicador.id = ?1", Sort.by("dataHora").descending(), indicadorId)
                .firstResultOptional();
    }

    public Optional<Cotacao> buscarUltimaCotacaoPorFonte(Long indicadorId, FonteDados fonte) {
        return Cotacao.<Cotacao>find(
                "indicador.id = ?1 and fonte = ?2",
                Sort.by("dataHora").descending(),
                indicadorId, fonte
        ).firstResultOptional();
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

        String sql = "SELECT timestamp_em_segundos, v FROM ("
                + "SELECT UNIX_TIMESTAMP(" + expressaoTruncamento + ") AS timestamp_em_segundos, valor AS v, "
                + "ROW_NUMBER() OVER (PARTITION BY " + expressaoTruncamento + " ORDER BY data_hora DESC, id DESC) AS rn "
                + "FROM cotacao "
                + "WHERE indicador_id = :indicadorId AND data_hora BETWEEN :inicio AND :fim"
                + ") agregado "
                + "WHERE rn = 1 "
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

    public MinMaxCotacao buscarMinMax(Long indicadorId, Instant inicio, Instant fim) {
        String sql = "SELECT MIN(valor) AS minimo, MAX(valor) AS maximo "
                + "FROM cotacao "
                + "WHERE indicador_id = :indicadorId AND data_hora BETWEEN :inicio AND :fim";

        Tuple linha = (Tuple) Cotacao.getEntityManager()
                .createNativeQuery(sql, Tuple.class)
                .setParameter("indicadorId", indicadorId)
                .setParameter("inicio", inicio)
                .setParameter("fim", fim)
                .getSingleResult();

        BigDecimal minimo = linha.get("minimo", BigDecimal.class);
        BigDecimal maximo = linha.get("maximo", BigDecimal.class);

        return new MinMaxCotacao(
                minimo == null ? null : minimo.setScale(6, RoundingMode.HALF_UP),
                maximo == null ? null : maximo.setScale(6, RoundingMode.HALF_UP)
        );
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
