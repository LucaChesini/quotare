package com.quotare.cotacoes.domain;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class IndicadorCotacaoPersistenceTest {

    @Inject
    EntityManager entityManager;

    @Test
    @TestTransaction
    void persistirIndicadorGeraIdViaAutoIncrement() {
        var indicador = new Indicador();
        indicador.codigo = "TEST";
        indicador.nome = "Indicador de Teste";
        indicador.fonte = FonteDados.LOCAL;
        indicador.ativo = true;

        assertNotNull(indicador);
        indicador.persist();

        assertNotNull(indicador.id);
        assertTrue(indicador.id > 0);
    }

    @Test
    @TestTransaction
    void persistirCotacaoAssociadaAoIndicadorEContarPorIndicador() {
        var indicador = new Indicador();
        indicador.codigo = "TST2";
        indicador.nome = "Indicador de Teste 2";
        indicador.fonte = FonteDados.LOCAL;
        indicador.ativo = true;
        indicador.persist();

        var cotacao = new Cotacao();
        cotacao.indicador = indicador;
        cotacao.valor = new BigDecimal("123.456700");
        Instant dataHoraOriginal = Instant.parse("2026-08-20T10:00:00Z");
        cotacao.dataHora = dataHoraOriginal;
        cotacao.fonte = FonteDados.LOCAL;
        cotacao.persist();

        assertNotNull(cotacao.id);

        entityManager.flush();
        entityManager.clear();

        var recarregada = Cotacao.<Cotacao>findById(cotacao.id);
        assertEquals(0, new BigDecimal("123.456700").compareTo(recarregada.valor));
        assertEquals(FonteDados.LOCAL, recarregada.fonte);
        assertEquals(dataHoraOriginal, recarregada.dataHora);

        long total = Cotacao.count("indicador.id", indicador.id);
        assertEquals(1L, total);
    }
}
