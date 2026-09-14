package com.quotare.cotacoes.mapper;

import com.quotare.cotacoes.domain.Cotacao;
import com.quotare.cotacoes.domain.FonteDados;
import com.quotare.cotacoes.domain.Indicador;
import com.quotare.cotacoes.dto.AtualizarCotacaoRequest;
import com.quotare.cotacoes.dto.CriarCotacaoRequest;
import com.quotare.cotacoes.dto.CotacaoResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("CotacaoMapper")
class CotacaoMapperTest {

    private final CotacaoMapper mapper = new CotacaoMapperImpl(new IndicadorMapperImpl());

    @Nested
    @DisplayName("toResponse")
    class ToResponse {

        @Test
        @DisplayName("copia indicadorId a partir de cotacao.indicador.id, não do id da própria cotação")
        void copiaIndicadorIdDoIndicadorAssociado() {
            var indicador = new Indicador();
            indicador.id = 99L;
            indicador.codigo = "USD-BRL";
            indicador.nome = "Dólar Comercial";

            var cotacao = new Cotacao();
            cotacao.id = 1L;
            cotacao.indicador = indicador;
            cotacao.valor = new BigDecimal("5.432100");
            cotacao.dataHora = Instant.now().minus(1, ChronoUnit.DAYS);
            cotacao.fonte = FonteDados.LOCAL;
            cotacao.criadoEm = Instant.now();

            CotacaoResponse response = mapper.toResponse(cotacao);

            assertEquals(indicador.id, response.indicadorId(), "indicadorId deveria vir de cotacao.indicador.id, não de cotacao.id");
            assertEquals(indicador.codigo, response.indicador().codigo(), "indicador.codigo deveria vir do IndicadorMapper composto");
            assertEquals(indicador.nome, response.indicador().nome(), "indicador.nome deveria vir do IndicadorMapper composto");
            assertEquals(cotacao.id, response.id(), "id deveria ser o id da própria cotação");
            assertEquals(0, cotacao.valor.compareTo(response.valor()), "valor deveria ser copiado");
            assertEquals(cotacao.dataHora, response.dataHora(), "dataHora deveria ser copiada");
            assertEquals(cotacao.fonte, response.fonte(), "fonte deveria ser copiada");
            assertEquals(cotacao.criadoEm, response.criadoEm(), "criadoEm deveria ser copiado");
        }
    }

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("copia valor e dataHora do request, mas não atribui indicador a partir do indicadorId")
        void copiaValorEDataHoraSemAtribuirIndicador() {
            var request = new CriarCotacaoRequest(7L, new BigDecimal("3.140000"), Instant.parse("2024-01-01T00:00:00Z"));

            Cotacao cotacao = mapper.toEntity(request);

            assertEquals(0, request.valor().compareTo(cotacao.valor), "valor deveria ser copiado");
            assertEquals(request.dataHora(), cotacao.dataHora, "dataHora deveria ser copiada");
            assertNull(cotacao.indicador, "indicador deveria ficar nulo: indicadorId não é resolvido pelo mapper, e sim pelo service");
            assertNull(cotacao.id, "id deveria ficar nulo (gerado pelo banco)");
            assertNull(cotacao.fonte, "fonte deveria ficar nula: é responsabilidade do service atribuir LOCAL");
            assertNull(cotacao.criadoEm, "criadoEm deveria ficar nulo (gerado pelo banco)");
        }
    }

    @Nested
    @DisplayName("atualizar")
    class Atualizar {

        @Test
        @DisplayName("sobrescreve valor e dataHora, mas não toca id/indicador/fonte/criadoEm")
        void sobrescreveValorEDataHoraPreservandoOResto() {
            var indicadorOriginal = new Indicador();
            indicadorOriginal.id = 42L;

            var cotacao = new Cotacao();
            cotacao.id = 10L;
            cotacao.indicador = indicadorOriginal;
            cotacao.valor = new BigDecimal("1.000000");
            cotacao.dataHora = Instant.parse("2024-01-01T00:00:00Z");
            cotacao.fonte = FonteDados.LOCAL;
            cotacao.criadoEm = Instant.parse("2023-12-31T00:00:00Z");

            var idOriginal = cotacao.id;
            var indicadorOriginalReferencia = cotacao.indicador;
            var fonteOriginal = cotacao.fonte;
            var criadoEmOriginal = cotacao.criadoEm;

            var request = new AtualizarCotacaoRequest(999L, new BigDecimal("2.500000"), Instant.parse("2024-02-02T00:00:00Z"));

            mapper.atualizar(request, cotacao);

            assertEquals(0, request.valor().compareTo(cotacao.valor), "valor deveria ser sobrescrito");
            assertEquals(request.dataHora(), cotacao.dataHora, "dataHora deveria ser sobrescrita");
            assertEquals(idOriginal, cotacao.id, "id não deveria ser alterado pelo mapper");
            assertEquals(indicadorOriginalReferencia, cotacao.indicador, "indicador não deveria ser alterado pelo mapper: o service resolve o novo Indicador e atribui depois");
            assertEquals(fonteOriginal, cotacao.fonte, "fonte não deveria ser alterada pelo mapper");
            assertEquals(criadoEmOriginal, cotacao.criadoEm, "criadoEm não deveria ser alterado pelo mapper");
        }
    }
}
