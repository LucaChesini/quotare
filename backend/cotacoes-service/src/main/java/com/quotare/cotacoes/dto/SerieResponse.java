package com.quotare.cotacoes.dto;

import java.util.List;

public record SerieResponse(
        IndicadorResumoResponse indicador,
        GranularidadeSerie granularidade,
        List<PontoResponse> pontos,
        ResumoResponse resumo
) {
}
