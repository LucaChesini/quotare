package com.quotare.cotacoes.client;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CotacaoExternaDTO(
    String codigo,
    BigDecimal valor,
    Instant dataHora
) {
}
