package com.quotare.cotacoes.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@QuarkusTest
@DisplayName("CotacaoExternaDTO tolera campos desconhecidos mesmo com fail-on-unknown-properties global")
class CotacaoExternaDTOTest {

    @Inject
    ObjectMapper objectMapper;

    @Test
    @DisplayName("desserializa JSON com campo extra do integracao-service sem lançar exceção, provando que @JsonIgnoreProperties sobrepõe quarkus.jackson.fail-on-unknown-properties")
    void desserializaComCampoExtraSemFalhar() throws Exception {
        String jsonComCampoNovo = """
            {
              "codigo": "USD",
              "valor": 5.25,
              "dataHora": "2026-09-15T12:00:00Z",
              "campoNovoDoIntegracaoService": "qualquer valor"
            }
            """;

        CotacaoExternaDTO dto = assertDoesNotThrow(
            () -> objectMapper.readValue(jsonComCampoNovo, CotacaoExternaDTO.class)
        );

        assertEquals("USD", dto.codigo());
        assertEquals(0, new BigDecimal("5.25").compareTo(dto.valor()));
    }
}
