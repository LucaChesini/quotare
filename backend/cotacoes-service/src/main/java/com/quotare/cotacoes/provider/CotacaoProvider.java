package com.quotare.cotacoes.provider;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.quotare.cotacoes.domain.FonteDados;
import com.quotare.cotacoes.dto.GranularidadeSerie;

public interface CotacaoProvider {

    List<CotacaoDTO> buscarSerie(String codigoIndicador, Instant inicio, Instant fim, GranularidadeSerie granularidade);

    Optional<CotacaoDTO> buscarUltima(String codigoIndicador);

    List<IndicadorDTO> listarIndicadoresDisponiveis();

    boolean suporta(FonteDados fonte);
}
