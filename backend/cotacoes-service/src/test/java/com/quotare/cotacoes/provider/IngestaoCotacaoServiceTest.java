package com.quotare.cotacoes.provider;

import com.quotare.cotacoes.client.CotacaoExternaDTO;
import com.quotare.cotacoes.domain.Cotacao;
import com.quotare.cotacoes.domain.FonteDados;
import com.quotare.cotacoes.domain.Indicador;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@DisplayName("IngestaoCotacaoService")
class IngestaoCotacaoServiceTest {

    @Inject
    IngestaoCotacaoService ingestaoService;

    @BeforeEach
    void limparBanco() {
        QuarkusTransaction.requiringNew().run(() -> {
            Cotacao.deleteAll();
            Indicador.deleteAll();
        });
    }

    private Indicador persistirIndicador(String codigo, String nome, FonteDados fonte) {
        return QuarkusTransaction.requiringNew().call(() -> {
            var indicador = new Indicador();
            indicador.codigo = codigo;
            indicador.nome = nome;
            indicador.fonte = fonte;
            indicador.ativo = true;
            indicador.persist();
            return indicador;
        });
    }

    private Cotacao persistirCotacao(Indicador indicador, FonteDados fonte, BigDecimal valor, Instant dataHora) {
        return QuarkusTransaction.requiringNew().call(() -> {
            var cotacao = new Cotacao();
            cotacao.indicador = indicador;
            cotacao.valor = valor;
            cotacao.dataHora = dataHora;
            cotacao.fonte = fonte;
            cotacao.persist();
            return cotacao;
        });
    }

    @Test
    @DisplayName("com cotação EXTERNA prévia, inicio é a data_hora dela, ignorando LOCAL mais recente")
    void comCotacaoExternaPreviaRetornaSuaDataHora() {
        var indicador = persistirIndicador("ING1", "Indicador com histórico EXTERNA", FonteDados.EXTERNA);

        var dataHoraExterna = Instant.parse("2024-06-01T12:00:00Z");
        persistirCotacao(indicador, FonteDados.EXTERNA, new BigDecimal("10.00"), dataHoraExterna);

        var dataHoraLocalMaisRecente = dataHoraExterna.plus(1, ChronoUnit.DAYS);
        persistirCotacao(indicador, FonteDados.LOCAL, new BigDecimal("99.00"), dataHoraLocalMaisRecente);

        Instant inicio = ingestaoService.inicioDaJanela(indicador);

        assertEquals(dataHoraExterna, inicio);
    }

    @Test
    @DisplayName("sem cotação EXTERNA (nunca ingerido), inicio é agora menos a janela inicial configurada")
    void semCotacaoExternaRetornaAgoraMenosJanelaInicial() {
        var indicador = persistirIndicador("ING2", "Indicador nunca ingerido", FonteDados.EXTERNA);

        var antesDaChamada = Instant.now();

        Instant inicio = ingestaoService.inicioDaJanela(indicador);

        var esperado = antesDaChamada.minus(Duration.parse("P30D"));

        assertTrue(
                Duration.between(esperado, inicio).abs().compareTo(Duration.ofSeconds(5)) < 0,
                "inicio deveria estar a cerca de 30 dias no passado, mas foi " + inicio
        );
    }

    @Test
    @DisplayName("upsertDeIngestao insere pontos novos com fonte EXTERNA")
    void upsertDeIngestaoInsereNovosPontos() {
        var indicador = persistirIndicador("ING3", "Indicador para upsert", FonteDados.EXTERNA);

        var pontos = List.of(
                new CotacaoExternaDTO("ING3", new BigDecimal("5.10"), Instant.parse("2024-06-01T10:00:00Z")),
                new CotacaoExternaDTO("ING3", new BigDecimal("5.20"), Instant.parse("2024-06-01T11:00:00Z"))
        );

        ingestaoService.upsertDeIngestao(indicador.id, pontos);

        long total = QuarkusTransaction.requiringNew().call(() ->
                Cotacao.count("indicador.id = :indicadorId and fonte = :fonte",
                        Map.of("indicadorId", indicador.id, "fonte", FonteDados.EXTERNA))
        );

        assertEquals(2, total);
    }

    @Test
    @DisplayName("upsertDeIngestao chamado duas vezes com o mesmo ponto não duplica, atualiza o valor")
    void upsertDeIngestaoNaoDuplicaEAtualizaValor() {
        var indicador = persistirIndicador("ING4", "Indicador para upsert repetido", FonteDados.EXTERNA);
        var dataHora = Instant.parse("2024-06-01T10:00:00Z");

        var primeiraVersao = List.of(new CotacaoExternaDTO("ING4", new BigDecimal("7.00"), dataHora));
        ingestaoService.upsertDeIngestao(indicador.id, primeiraVersao);

        var segundaVersao = List.of(new CotacaoExternaDTO("ING4", new BigDecimal("8.50"), dataHora));
        ingestaoService.upsertDeIngestao(indicador.id, segundaVersao);

        long total = QuarkusTransaction.requiringNew().call(() ->
                Cotacao.count("indicador.id = :indicadorId and fonte = :fonte",
                        Map.of("indicadorId", indicador.id, "fonte", FonteDados.EXTERNA))
        );
        assertEquals(1, total);

        BigDecimal valorGravado = QuarkusTransaction.requiringNew().call(() ->
                Cotacao.<Cotacao>find("indicador.id = :indicadorId and fonte = :fonte",
                                Map.of("indicadorId", indicador.id, "fonte", FonteDados.EXTERNA))
                        .firstResult().valor
        );

        assertEquals(0, new BigDecimal("8.50").compareTo(valorGravado));
    }

    @Test
    @DisplayName("upsertDeIngestao com lista vazia não insere nada e não lança exceção")
    void upsertDeIngestaoComListaVaziaNaoFazNada() {
        var indicador = persistirIndicador("ING5", "Indicador sem pontos novos", FonteDados.EXTERNA);

        ingestaoService.upsertDeIngestao(indicador.id, List.of());

        long total = QuarkusTransaction.requiringNew().call(() ->
                Cotacao.count("indicador.id = :indicadorId", Map.of("indicadorId", indicador.id))
        );

        assertEquals(0, total);
    }
}
