package com.quotare.cotacoes.provider;

import com.quotare.cotacoes.domain.Cotacao;
import com.quotare.cotacoes.domain.FonteDados;
import com.quotare.cotacoes.domain.Indicador;
import com.quotare.cotacoes.dto.GranularidadeSerie;
import com.quotare.cotacoes.dto.PontoResponse;

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
@DisplayName("CotacaoSerieRepository")
class CotacaoSerieRepositoryTest {

    @Inject
    CotacaoSerieRepository serieRepository;

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
        return persistirCotacao(indicador, valor, dataHora, FonteDados.LOCAL);
    }

    private Cotacao persistirCotacao(Indicador indicador, BigDecimal valor, Instant dataHora, FonteDados fonte) {
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
    @DisplayName("agrega por DIA usando o último valor cronológico (close) do dia")
    void agregaPorDiaUsandoOUltimoValorCronologicoDoDia() {
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
        List<PontoResponse> pontos = serieRepository.buscarPontos(indicador.id, inicio, fim, GranularidadeSerie.DIA);

        assertEquals(2, pontos.size());

        assertTrue(pontos.get(0).t().isBefore(pontos.get(1).t()));

        BigDecimal closeDia1 = new BigDecimal("30").setScale(6, RoundingMode.HALF_UP);
        BigDecimal closeDia2 = new BigDecimal("15").setScale(6, RoundingMode.HALF_UP);

        assertEquals(0, closeDia1.compareTo(pontos.get(0).v()), "close do dia 1 deveria ser o valor das 20h (30)");
        assertEquals(0, closeDia2.compareTo(pontos.get(1).v()), "close do dia 2 deveria ser o valor das 18h (15)");
    }

    @Test
    @DisplayName("em empate de data_hora no mesmo período, o close usa o maior id (a cotação persistida por último)")
    void emEmpateDeDataHoraOCloseUsaOMaiorId() {
        var indicador = persistirIndicador("AGR3", "Indicador Empate Close");

        var dataHora = Instant.parse("2024-03-05T12:00:00Z");
        persistirCotacao(indicador, new BigDecimal("100"), dataHora, FonteDados.EXTERNA);
        var ultimaPersistida = persistirCotacao(indicador, new BigDecimal("200"), dataHora, FonteDados.LOCAL);

        assertTrue(ultimaPersistida.id != null);

        var inicio = dataHora.minus(1, ChronoUnit.HOURS);
        var fim = dataHora.plus(1, ChronoUnit.HOURS);
        List<PontoResponse> pontos = serieRepository.buscarPontos(indicador.id, inicio, fim, GranularidadeSerie.DIA);

        assertEquals(1, pontos.size());
        assertEquals(0, new BigDecimal("200.000000").compareTo(pontos.get(0).v()),
                "deveria vencer o valor da cotação com maior id, persistida por último");
    }

    @Test
    @DisplayName("agrega por SEMANA usando o último valor cronológico (close) da semana, respeitando a expressão de truncamento")
    void agregaPorSemanaUsandoOUltimoValorCronologicoDaSemana() {
        var indicador = persistirIndicador("AGR4", "Indicador Agregação Semana");

        var segundaFeira = Instant.parse("2024-03-04T00:00:00Z");
        persistirCotacao(indicador, new BigDecimal("1"), segundaFeira.plus(1, ChronoUnit.HOURS));
        persistirCotacao(indicador, new BigDecimal("2"), segundaFeira.plus(3, ChronoUnit.DAYS));

        var proximaSegunda = segundaFeira.plus(7, ChronoUnit.DAYS);
        persistirCotacao(indicador, new BigDecimal("3"), proximaSegunda.plus(2, ChronoUnit.HOURS));

        var inicio = segundaFeira;
        var fim = proximaSegunda.plus(1, ChronoUnit.DAYS);
        List<PontoResponse> pontos = serieRepository.buscarPontos(indicador.id, inicio, fim, GranularidadeSerie.SEMANA);

        assertEquals(2, pontos.size());
        assertTrue(pontos.get(0).t().isBefore(pontos.get(1).t()));

        assertEquals(0, new BigDecimal("2.000000").compareTo(pontos.get(0).v()),
                "close da primeira semana deveria ser o valor de quinta-feira (2)");
        assertEquals(0, new BigDecimal("3.000000").compareTo(pontos.get(1).v()),
                "close da segunda semana deveria ser o único valor lançado nela (3)");
    }

    @Test
    @DisplayName("retorna mínimo e máximo nulos quando não há cotações no período")
    void retornaMinimoEMaximoNulosQuandoNaoHaCotacoesNoPeriodo() {
        var indicador = persistirIndicador("MINMAX2", "Indicador Sem Cotações Min/Max");

        var inicio = Instant.parse("2024-07-01T00:00:00Z");
        var fim = Instant.parse("2024-07-05T00:00:00Z");

        var minMax = serieRepository.buscarMinMax(indicador.id, inicio, fim);

        assertNull(minMax.minimo());
        assertNull(minMax.maximo());
    }
}
