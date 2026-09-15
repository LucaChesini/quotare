package com.quotare.cotacoes.service;

import com.quotare.cotacoes.domain.Cotacao;
import com.quotare.cotacoes.domain.FonteDados;
import com.quotare.cotacoes.domain.Indicador;
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
@DisplayName("CotacaoService - seleção de provider por fonte do indicador")
class CotacaoServiceProviderIntegrationTest {

    @Inject
    CotacaoService service;

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

    private Cotacao persistirCotacao(Indicador indicador, BigDecimal valor, Instant dataHora) {
        return QuarkusTransaction.requiringNew().call(() -> {
            var cotacao = new Cotacao();
            cotacao.indicador = indicador;
            cotacao.valor = valor;
            cotacao.dataHora = dataHora;
            cotacao.fonte = indicador.fonte;
            cotacao.persist();
            return cotacao;
        });
    }

    @Test
    @DisplayName("indicador LOCAL é atendido pelo LocalCotacaoProvider via buscarSerie")
    void indicadorLocalEAtendidoPeloLocalCotacaoProvider() {
        var indicador = persistirIndicador("PROV-LOCAL", "Indicador Local", FonteDados.LOCAL);

        var inicio = Instant.parse("2024-06-01T00:00:00Z");
        persistirCotacao(indicador, new BigDecimal("10.00"), inicio.plus(1, ChronoUnit.HOURS));
        persistirCotacao(indicador, new BigDecimal("11.00"), inicio.plus(2, ChronoUnit.HOURS));
        persistirCotacao(indicador, new BigDecimal("12.00"), inicio.plus(3, ChronoUnit.HOURS));

        var fim = inicio.plus(1, ChronoUnit.DAYS);
        SerieResponse serie = service.buscarSerie(indicador.id, inicio, fim, GranularidadeSerie.BRUTO);

        List<PontoResponse> pontos = serie.pontos();
        assertEquals(3, pontos.size());
        assertEquals(0, new BigDecimal("10.00").compareTo(pontos.get(0).v()));
        assertEquals(0, new BigDecimal("11.00").compareTo(pontos.get(1).v()));
        assertEquals(0, new BigDecimal("12.00").compareTo(pontos.get(2).v()));
        assertTrue(pontos.get(0).t().isBefore(pontos.get(1).t()));
        assertTrue(pontos.get(1).t().isBefore(pontos.get(2).t()));

        CotacaoProvider provider = providerFactory.para(indicador.fonte);
        assertInstanceOf(LocalCotacaoProvider.class, provider);
    }

    @Test
    @DisplayName("indicador EXTERNA é atendido pelo ExternaCotacaoProvider via buscarSerie")
    void indicadorExternaEAtendidoPeloExternaCotacaoProvider() {
        var indicador = persistirIndicador("PROV-EXT", "Indicador Externo", FonteDados.EXTERNA);

        var inicio = Instant.parse("2024-06-01T00:00:00Z");
        persistirCotacao(indicador, new BigDecimal("5.10"), inicio.plus(1, ChronoUnit.HOURS));
        persistirCotacao(indicador, new BigDecimal("5.25"), inicio.plus(2, ChronoUnit.HOURS));

        var fim = inicio.plus(1, ChronoUnit.DAYS);
        SerieResponse serie = service.buscarSerie(indicador.id, inicio, fim, GranularidadeSerie.BRUTO);

        List<PontoResponse> pontos = serie.pontos();
        assertEquals(2, pontos.size());
        assertEquals(0, new BigDecimal("5.10").compareTo(pontos.get(0).v()));
        assertEquals(0, new BigDecimal("5.25").compareTo(pontos.get(1).v()));
        assertTrue(pontos.get(0).t().isBefore(pontos.get(1).t()));

        CotacaoProvider provider = providerFactory.para(indicador.fonte);
        assertInstanceOf(ExternaCotacaoProvider.class, provider);
    }
}
