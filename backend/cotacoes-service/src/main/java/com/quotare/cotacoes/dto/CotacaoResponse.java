package com.quotare.cotacoes.dto;

import com.quotare.cotacoes.domain.FonteDados;

import java.math.BigDecimal;
import java.time.Instant;

public record CotacaoResponse(
        Long id,
        Long indicadorId,
        BigDecimal valor,
        Instant dataHora,
        FonteDados fonte,
        Instant criadoEm
) {
}
