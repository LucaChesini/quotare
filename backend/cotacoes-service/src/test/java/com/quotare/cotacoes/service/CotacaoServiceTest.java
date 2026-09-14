package com.quotare.cotacoes.service;

import com.quotare.cotacoes.domain.Cotacao;
import com.quotare.cotacoes.domain.FonteDados;
import com.quotare.cotacoes.domain.Indicador;
import com.quotare.cotacoes.dto.GranularidadeSerie;
import com.quotare.cotacoes.dto.PontoResponse;
import com.quotare.cotacoes.dto.ResumoResponse;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    private Cotacao persistirCotacao(Indicador indicador, BigDecimal valor, Instant dataHora) {
        return QuarkusTransaction.requiringNew().call(() -> {
            var cotacao = new Cotacao();
            cotacao.indicador = indicador;
            cotacao.valor = valor;
            cotacao.dataHora = dataHora;
            cotacao.fonte = FonteDados.LOCAL;
            cotacao.persist();
            return cotacao;
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

    @Test
    @DisplayName("agrega por DIA calculando a média dos valores lançados no mesmo dia")
    void agregaPorDiaCalculandoAMediaDosValoresDoMesmoDia() {
        var indicador = persistirIndicador("AGR1", "Indicador Agregação Dia");

        var dia1 = Instant.parse("2024-03-01T00:00:00Z");
        persistirCotacao(indicador, new BigDecimal("10"), dia1.plus(1, ChronoUnit.HOURS));
        persistirCotacao(indicador, new BigDecimal("20"), dia1.plus(10, ChronoUnit.HOURS));
        persistirCotacao(indicador, new BigDecimal("30"), dia1.plus(20, ChronoUnit.HOURS));

        var dia2 = dia1.plus(1, ChronoUnit.DAYS);
        persistirCotacao(indicador, new BigDecimal("5"), dia2.plus(2, ChronoUnit.HOURS));
        persistirCotacao(indicador, new BigDecimal("15"), dia2.plus(18, ChronoUnit.HOURS));

        var inicio = dia1;
        var fim = dia2.plus(1, ChronoUnit.DAYS);
        List<PontoResponse> pontos = service.buscarPontos(indicador.id, inicio, fim, GranularidadeSerie.DIA);

        assertEquals(2, pontos.size());

        assertTrue(pontos.get(0).t().isBefore(pontos.get(1).t()));

        BigDecimal mediaDia1 = new BigDecimal("10").add(new BigDecimal("20")).add(new BigDecimal("30"))
                .divide(new BigDecimal("3"), 6, RoundingMode.HALF_UP);
        BigDecimal mediaDia2 = new BigDecimal("5").add(new BigDecimal("15"))
                .divide(new BigDecimal("2"), 6, RoundingMode.HALF_UP);

        assertEquals(0, mediaDia1.compareTo(pontos.get(0).v()), "média do dia 1 deveria ser 20");
        assertEquals(0, mediaDia2.compareTo(pontos.get(1).v()), "média do dia 2 deveria ser 10");
    }

    @Test
    @DisplayName("retorna lista de pontos vazia e resumo totalmente nulo quando não há cotações no período")
    void retornaListaVaziaEResumoNuloQuandoNaoHaCotacoesNoPeriodo() {
        var indicador = persistirIndicador("AGR2", "Indicador Sem Cotações");

        var inicio = Instant.parse("2024-04-01T00:00:00Z");
        var fim = Instant.parse("2024-04-05T00:00:00Z");
        var serie = service.buscarSerie(indicador.id, inicio, fim, GranularidadeSerie.DIA);

        assertTrue(serie.pontos().isEmpty());
        assertNull(serie.resumo().minimo());
        assertNull(serie.resumo().maximo());
        assertNull(serie.resumo().variacaoPercentual());
    }

    @Test
    @DisplayName("não calcula variação percentual quando o primeiro valor cronológico é zero, mas mínimo/máximo continuam calculados")
    void naoCalculaVariacaoPercentualQuandoPrimeiroValorEZero() throws Exception {
        var inicio = Instant.parse("2024-05-01T00:00:00Z");
        List<PontoResponse> pontos = List.of(
                new PontoResponse(inicio, BigDecimal.ZERO),
                new PontoResponse(inicio.plus(1, ChronoUnit.HOURS), new BigDecimal("50"))
        );

        ResumoResponse resumo = invocarCalcularResumo(pontos);

        assertNull(resumo.variacaoPercentual());
        assertEquals(0, BigDecimal.ZERO.compareTo(resumo.minimo()));
        assertEquals(0, new BigDecimal("50").compareTo(resumo.maximo()));
    }

    private ResumoResponse invocarCalcularResumo(List<PontoResponse> pontos) throws Exception {
        var metodo = CotacaoService.class.getDeclaredMethod("calcularResumo", List.class);
        metodo.setAccessible(true);
        return (ResumoResponse) metodo.invoke(service, pontos);
    }
}
