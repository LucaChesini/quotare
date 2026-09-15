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
        List<PontoResponse> pontos = serieRepository.buscarPontos(indicador.id, inicio, fim, GranularidadeSerie.DIA);

        assertEquals(2, pontos.size());

        assertTrue(pontos.get(0).t().isBefore(pontos.get(1).t()));

        BigDecimal mediaDia1 = new BigDecimal("10").add(new BigDecimal("20")).add(new BigDecimal("30"))
                .divide(new BigDecimal("3"), 6, RoundingMode.HALF_UP);
        BigDecimal mediaDia2 = new BigDecimal("5").add(new BigDecimal("15"))
                .divide(new BigDecimal("2"), 6, RoundingMode.HALF_UP);

        assertEquals(0, mediaDia1.compareTo(pontos.get(0).v()), "média do dia 1 deveria ser 20");
        assertEquals(0, mediaDia2.compareTo(pontos.get(1).v()), "média do dia 2 deveria ser 10");
    }
}
