package com.quotare.cotacoes.dto;

import com.quotare.cotacoes.domain.FonteDados;

import java.math.BigDecimal;
import java.time.Instant;

public record CotacaoResponse(
        Long id,
        Long indicadorId,
        IndicadorResumoResponse indicador,
        BigDecimal valor,
        Instant dataHora,
        FonteDados fonte,
        Instant criadoEm
) {
}
