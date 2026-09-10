package com.quotare.cotacoes.dto;

import com.quotare.cotacoes.domain.FonteDados;

import java.time.Instant;

public record IndicadorResponse(
        Long id,
        String codigo,
        String nome,
        FonteDados fonte,
        Boolean ativo,
        Instant criadoEm,
        Instant atualizadoEm
) {
}
