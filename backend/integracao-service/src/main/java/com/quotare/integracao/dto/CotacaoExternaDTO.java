package com.quotare.integracao.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CotacaoExternaDTO(
    String codigo,
    BigDecimal valor,
    Instant dataHora
) {
}
