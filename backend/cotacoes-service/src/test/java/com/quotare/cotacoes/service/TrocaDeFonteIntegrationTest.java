package com.quotare.cotacoes.service;

import com.quotare.cotacoes.domain.Cotacao;
import com.quotare.cotacoes.domain.FonteDados;
import com.quotare.cotacoes.domain.Indicador;
import com.quotare.cotacoes.dto.AtualizarIndicadorRequest;
import com.quotare.cotacoes.dto.GranularidadeSerie;
import com.quotare.cotacoes.dto.PontoResponse;
import com.quotare.cotacoes.dto.SerieResponse;
import com.quotare.cotacoes.provider.CotacaoProvider;
import com.quotare.cotacoes.provider.CotacaoProviderFactory;
import com.quotare.cotacoes.provider.ExternaCotacaoProvider;
import com.quotare.cotacoes.provider.LocalCotacaoProvider;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@DisplayName("Troca de fonte do indicador em runtime (LOCAL -> EXTERNA)")
class TrocaDeFonteIntegrationTest {

    @Inject
    CotacaoService service;

    @Inject
    IndicadorService indicadorService;

    @Inject
    CotacaoProviderFactory providerFactory;

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

    private Indicador recarregarIndicador(Long id) {
        return QuarkusTransaction.requiringNew().call(() -> Indicador.<Indicador>findById(id));
    }

    @Test
    @DisplayName("cotações LOCAL antigas sobrevivem à troca de fonte e se juntam às EXTERNA novas na mesma série")
    void trocaDeFontePreservaHistoricoLocalEPassaAAtenderEmExterna() {
        var indicador = persistirIndicador("TROCA1", "Indicador com troca de fonte", FonteDados.LOCAL);

        var inicioLocal = Instant.parse("2024-06-01T00:00:00Z");
        persistirCotacao(indicador, FonteDados.LOCAL, new BigDecimal("11.00"), inicioLocal.plus(1, ChronoUnit.HOURS));
        persistirCotacao(indicador, FonteDados.LOCAL, new BigDecimal("10.00"), inicioLocal);

        var fimFase1 = inicioLocal.plus(1, ChronoUnit.DAYS);
        SerieResponse serieAntesDaTroca = service.buscarSerie(indicador.id, inicioLocal, fimFase1, GranularidadeSerie.BRUTO);

        List<PontoResponse> pontosAntesDaTroca = serieAntesDaTroca.pontos();
        assertEquals(2, pontosAntesDaTroca.size());
        assertEquals(0, new BigDecimal("10.00").compareTo(pontosAntesDaTroca.get(0).v()));
        assertEquals(0, new BigDecimal("11.00").compareTo(pontosAntesDaTroca.get(1).v()));

        CotacaoProvider providerAntesDaTroca = providerFactory.para(recarregarIndicador(indicador.id).fonte);
        assertInstanceOf(LocalCotacaoProvider.class, providerAntesDaTroca);

        indicadorService.atualizar(
                indicador.id,
                new AtualizarIndicadorRequest(indicador.codigo, indicador.nome, FonteDados.EXTERNA, true)
        );

        var inicioExterna = Instant.parse("2024-06-02T00:00:00Z");
        persistirCotacao(indicador, FonteDados.EXTERNA, new BigDecimal("21.00"), inicioExterna.plus(1, ChronoUnit.HOURS));
        persistirCotacao(indicador, FonteDados.EXTERNA, new BigDecimal("20.00"), inicioExterna);

        var fimFase2 = inicioExterna.plus(1, ChronoUnit.DAYS);
        SerieResponse serieCompleta = service.buscarSerie(indicador.id, inicioLocal, fimFase2, GranularidadeSerie.BRUTO);

        List<PontoResponse> pontosCompletos = serieCompleta.pontos();
        assertEquals(4, pontosCompletos.size());

        assertEquals(0, new BigDecimal("10.00").compareTo(pontosCompletos.get(0).v()));
        assertEquals(0, new BigDecimal("11.00").compareTo(pontosCompletos.get(1).v()));
        assertEquals(0, new BigDecimal("20.00").compareTo(pontosCompletos.get(2).v()));
        assertEquals(0, new BigDecimal("21.00").compareTo(pontosCompletos.get(3).v()));

        assertTrue(pontosCompletos.get(0).t().isBefore(pontosCompletos.get(1).t()));
        assertTrue(pontosCompletos.get(1).t().isBefore(pontosCompletos.get(2).t()));
        assertTrue(pontosCompletos.get(2).t().isBefore(pontosCompletos.get(3).t()));

        CotacaoProvider providerDepoisDaTroca = providerFactory.para(recarregarIndicador(indicador.id).fonte);
        assertInstanceOf(ExternaCotacaoProvider.class, providerDepoisDaTroca);
    }
}
