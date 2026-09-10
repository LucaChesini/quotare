package com.quotare.cotacoes.domain;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@QuarkusTest
class CotacaoTimezonePersistenceTest {

    @Inject
    EntityManager entityManager;

    @Test
    @TestTransaction
    void persistirInstantConhecidoDeveGravarColunaEmUtcSemDeslocamento() {
        assertNotEquals(0, TimeZone.getDefault().getRawOffset(),
                "A JVM de teste está em UTC (offset 0) — este teste não é significativo "
                        + "nessa condição. Verifique o -Duser.timezone configurado no "
                        + "maven-surefire-plugin (pom.xml).");

        var indicador = new Indicador();
        indicador.codigo = "UTCX";
        indicador.nome = "Indicador para teste de timezone";
        indicador.fonte = FonteDados.LOCAL;
        indicador.ativo = true;
        indicador.persist();

        Instant instanteConhecido = Instant.parse("2026-08-20T23:45:12.345678Z");

        var cotacao = new Cotacao();
        cotacao.indicador = indicador;
        cotacao.valor = new BigDecimal("100.000000");
        cotacao.dataHora = instanteConhecido;
        cotacao.fonte = FonteDados.LOCAL;
        cotacao.persist();

        entityManager.flush();

        String valorBrutoNaColuna = (String) entityManager
                .createNativeQuery("SELECT DATE_FORMAT(data_hora, '%Y-%m-%d %H:%i:%s.%f') FROM cotacao WHERE id = :id")
                .setParameter("id", cotacao.id)
                .getSingleResult();

        String esperadoEmUtc = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
                .withZone(java.time.ZoneOffset.UTC)
                .format(instanteConhecido);

        assertEquals(esperadoEmUtc, valorBrutoNaColuna);
    }
}
