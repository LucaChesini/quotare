package com.quotare.cotacoes.provider;

import com.quotare.cotacoes.domain.Cotacao;
import com.quotare.cotacoes.domain.FonteDados;
import com.quotare.cotacoes.domain.Indicador;
import com.quotare.cotacoes.dto.GranularidadeSerie;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@DisplayName("ExternaCotacaoProvider")
class ExternaCotacaoProviderTest {

    @Inject
    ExternaCotacaoProvider externaCotacaoProvider;

    @Inject
    CotacaoProviderFactory providerFactory;

    @BeforeEach
    void limparBanco() {
        QuarkusTransaction.requiringNew().run(() -> {
            Cotacao.deleteAll();
            Indicador.deleteAll();
        });
    }

    private Indicador persistirIndicador(String codigo, String nome, FonteDados fonte, boolean ativo) {
        return QuarkusTransaction.requiringNew().call(() -> {
            var indicador = new Indicador();
            indicador.codigo = codigo;
            indicador.nome = nome;
            indicador.fonte = fonte;
            indicador.ativo = ativo;
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
    @DisplayName("busca série com pontos brutos de um indicador EXTERNA")
    void buscaSerieComPontosBrutosDeUmIndicadorExterna() {
        var indicador = persistirIndicador("USD", "Dólar", FonteDados.EXTERNA, true);

        var inicio = Instant.parse("2024-03-01T00:00:00Z");
        persistirCotacao(indicador, new BigDecimal("5.10"), inicio.plus(1, ChronoUnit.HOURS));
        persistirCotacao(indicador, new BigDecimal("5.20"), inicio.plus(2, ChronoUnit.HOURS));

        var fim = inicio.plus(1, ChronoUnit.DAYS);
        List<CotacaoDTO> pontos = externaCotacaoProvider.buscarSerie("usd", inicio, fim, GranularidadeSerie.BRUTO);

        assertEquals(2, pontos.size());
        assertEquals(0, new BigDecimal("5.10").compareTo(pontos.get(0).valor()));
        assertEquals(0, new BigDecimal("5.20").compareTo(pontos.get(1).valor()));
        assertTrue(pontos.get(0).dataHora().isBefore(pontos.get(1).dataHora()));
    }

    @Test
    @DisplayName("suporta apenas a fonte EXTERNA")
    void suportaApenasFonteExterna() {
        assertTrue(externaCotacaoProvider.suporta(FonteDados.EXTERNA));
        assertFalse(externaCotacaoProvider.suporta(FonteDados.LOCAL));
    }

    @Test
    @DisplayName("lista apenas indicadores EXTERNA ativos, ignorando LOCAL")
    void listaApenasIndicadoresExternaAtivos() {
        persistirIndicador("USD", "Dólar", FonteDados.EXTERNA, true);
        persistirIndicador("EUR", "Euro", FonteDados.EXTERNA, false);
        persistirIndicador("PETR", "Petróleo", FonteDados.LOCAL, true);

        List<IndicadorDTO> disponiveis = externaCotacaoProvider.listarIndicadoresDisponiveis();

        assertEquals(1, disponiveis.size());
        assertEquals("USD", disponiveis.get(0).codigo());
    }

    @Test
    @DisplayName("factory resolve ExternaCotacaoProvider para a fonte EXTERNA")
    void factoryResolveExternaCotacaoProviderParaFonteExterna() {
        CotacaoProvider provider = providerFactory.para(FonteDados.EXTERNA);

        assertInstanceOf(ExternaCotacaoProvider.class, provider);
    }
}
