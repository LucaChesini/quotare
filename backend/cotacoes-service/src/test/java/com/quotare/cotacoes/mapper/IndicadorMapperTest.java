package com.quotare.cotacoes.mapper;

import com.quotare.cotacoes.domain.FonteDados;
import com.quotare.cotacoes.domain.Indicador;
import com.quotare.cotacoes.dto.AtualizarIndicadorRequest;
import com.quotare.cotacoes.dto.CriarIndicadorRequest;
import com.quotare.cotacoes.dto.IndicadorResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("IndicadorMapper")
class IndicadorMapperTest {

    private final IndicadorMapper mapper = new IndicadorMapperImpl();

    @Nested
    @DisplayName("toResponse")
    class ToResponse {

        @Test
        @DisplayName("copia todos os campos da entidade para o response")
        void copiaTodosOsCampos() {
            var indicador = new Indicador();
            indicador.id = 1L;
            indicador.codigo = "USD";
            indicador.nome = "Dólar Americano";
            indicador.fonte = FonteDados.EXTERNA;
            indicador.ativo = false;
            indicador.criadoEm = Instant.now().minus(1, ChronoUnit.DAYS);
            indicador.atualizadoEm = Instant.now();

            IndicadorResponse response = mapper.toResponse(indicador);

            assertEquals(indicador.id, response.id(), "id deveria ser copiado");
            assertEquals(indicador.codigo, response.codigo(), "codigo deveria ser copiado");
            assertEquals(indicador.nome, response.nome(), "nome deveria ser copiado");
            assertEquals(indicador.fonte, response.fonte(), "fonte deveria ser copiada");
            assertEquals(indicador.ativo, response.ativo(), "ativo deveria ser copiado");
            assertEquals(indicador.criadoEm, response.criadoEm(), "criadoEm deveria ser copiado");
            assertEquals(indicador.atualizadoEm, response.atualizadoEm(), "atualizadoEm deveria ser copiado");
        }
    }

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("copia codigo, nome e fonte, e deixa id/timestamps nulos e ativo verdadeiro")
        void copiaCamposDoRequest() {
            var request = new CriarIndicadorRequest("USD", "Dólar Americano", FonteDados.EXTERNA);

            Indicador indicador = mapper.toEntity(request);

            assertEquals(request.codigo(), indicador.codigo, "codigo deveria ser copiado");
            assertEquals(request.nome(), indicador.nome, "nome deveria ser copiado");
            assertEquals(request.fonte(), indicador.fonte, "fonte deveria ser copiada");
            assertNull(indicador.id, "id deveria ficar nulo (gerado pelo banco)");
            assertNull(indicador.criadoEm, "criadoEm deveria ficar nulo (gerado pelo banco)");
            assertNull(indicador.atualizadoEm, "atualizadoEm deveria ficar nulo (gerado pelo banco)");
            assertTrue(indicador.ativo, "ativo deveria manter o default true da entidade");
        }

        @Test
        @DisplayName("não normaliza o codigo para maiúsculo")
        void naoNormalizaCodigo() {
            var request = new CriarIndicadorRequest("usd", "Dólar Americano", FonteDados.EXTERNA);

            Indicador indicador = mapper.toEntity(request);

            assertEquals("usd", indicador.codigo, "normalização de maiúsculas é responsabilidade do service, não do mapper");
        }
    }

    @Nested
    @DisplayName("atualizar")
    class Atualizar {

        @Test
        @DisplayName("sobrescreve codigo, nome, fonte e ativo, preservando id e timestamps")
        void sobrescreveCamposEPreservaIdentidade() {
            var indicador = new Indicador();
            indicador.id = 42L;
            indicador.codigo = "USD";
            indicador.nome = "Dólar Americano";
            indicador.fonte = FonteDados.EXTERNA;
            indicador.ativo = true;
            indicador.criadoEm = Instant.now().minus(1, ChronoUnit.DAYS);
            indicador.atualizadoEm = Instant.now();

            var idOriginal = indicador.id;
            var criadoEmOriginal = indicador.criadoEm;
            var atualizadoEmOriginal = indicador.atualizadoEm;

            var request = new AtualizarIndicadorRequest("EUR", "Euro", FonteDados.LOCAL, false);

            mapper.atualizar(request, indicador);

            assertEquals("EUR", indicador.codigo, "codigo deveria ser sobrescrito");
            assertEquals("Euro", indicador.nome, "nome deveria ser sobrescrito");
            assertEquals(FonteDados.LOCAL, indicador.fonte, "fonte deveria ser sobrescrita");
            assertFalse(indicador.ativo, "ativo deveria ser sobrescrito (soft delete)");
            assertEquals(idOriginal, indicador.id, "id não deveria ser alterado");
            assertEquals(criadoEmOriginal, indicador.criadoEm, "criadoEm não deveria ser alterado");
            assertEquals(atualizadoEmOriginal, indicador.atualizadoEm, "atualizadoEm não deveria ser alterado");
        }
    }
}
