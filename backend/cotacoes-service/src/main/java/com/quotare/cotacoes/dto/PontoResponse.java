package com.quotare.cotacoes.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PontoResponse(
        Instant t,
        BigDecimal v
) {
}
