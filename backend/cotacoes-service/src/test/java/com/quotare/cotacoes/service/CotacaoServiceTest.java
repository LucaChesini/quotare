package com.quotare.cotacoes.service;

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

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@DisplayName("CotacaoService")
class CotacaoServiceTest {

    @Inject
    CotacaoService service;

    private static final int LIMITE_PONTOS = 2000;

    @BeforeEach
    void limparBanco() {
        QuarkusTransaction.requiringNew().run(() -> {
            Cotacao.deleteAll();
            Indicador.deleteAll();
        });
    }

    private Indicador persistirIndicador(String codigo, String nome) {
        return QuarkusTransaction.requiringNew().call(() -> {
            var indicador = new Indicador();
            indicador.codigo = codigo;
            indicador.nome = nome;
            indicador.fonte = FonteDados.LOCAL;
            indicador.ativo = true;
            indicador.persist();
            return indicador;
        });
    }

    private void persistirCotacoesEmMassa(Indicador indicador, Instant inicio, int quantidade) {
        QuarkusTransaction.requiringNew().run(() -> {
            for (int i = 0; i < quantidade; i++) {
                var cotacao = new Cotacao();
                cotacao.indicador = indicador;
                cotacao.valor = BigDecimal.ONE;
                cotacao.dataHora = inicio.plus(i, ChronoUnit.HOURS);
                cotacao.fonte = FonteDados.LOCAL;
                cotacao.persist();
            }
        });
    }

    @Test
    @DisplayName("retorna a granularidade solicitada sem promoção quando o volume de pontos está bem abaixo do limite")
    void retornaGranularidadeSolicitadaSemPromocaoQuandoVolumeBaixo() {
        var indicador = persistirIndicador("PROM1", "Indicador Promoção Baixa");
        var inicio = Instant.parse("2024-01-01T00:00:00Z");
        persistirCotacoesEmMassa(indicador, inicio, 5);

        var fim = inicio.plus(5, ChronoUnit.HOURS);
        var serie = service.buscarSerie(indicador.id, inicio, fim, GranularidadeSerie.HORA);

        assertEquals(GranularidadeSerie.HORA, serie.granularidade());
    }

    @Test
    @DisplayName("promove de HORA para DIA quando a contagem de pontos distintos ultrapassa o limite de 2000")
    void promoveDeHoraParaDiaQuandoContagemUltrapassaLimite() {
        var indicador = persistirIndicador("PROM2", "Indicador Promoção Alta");
        var inicio = Instant.parse("2020-01-01T00:00:00Z");

        int quantidade = LIMITE_PONTOS + 1;
        persistirCotacoesEmMassa(indicador, inicio, quantidade);

        var fim = inicio.plus(quantidade, ChronoUnit.HOURS);
        var serie = service.buscarSerie(indicador.id, inicio, fim, GranularidadeSerie.HORA);

        assertEquals(GranularidadeSerie.DIA, serie.granularidade());
    }

    @Test
    @DisplayName("não promove quando a contagem de pontos está exatamente no limite de 2000 (teste de borda real, viável graças à inserção em massa em uma única transação)")
    void naoPromoveQuandoContagemEstaExatamenteNoLimite() {
        var indicador = persistirIndicador("PROM3", "Indicador Promoção Limite");
        var inicio = Instant.parse("2021-01-01T00:00:00Z");

        int quantidade = LIMITE_PONTOS;
        persistirCotacoesEmMassa(indicador, inicio, quantidade);

        var fim = inicio.plus(quantidade, ChronoUnit.HOURS);
        var serie = service.buscarSerie(indicador.id, inicio, fim, GranularidadeSerie.HORA);

        assertEquals(GranularidadeSerie.HORA, serie.granularidade());
    }
}
