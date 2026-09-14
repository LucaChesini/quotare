package com.quotare.integracao.client;

import com.quotare.integracao.dto.CotacaoExternaDTO;

import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class ExternalQuoteMapper {

    public List<CotacaoExternaDTO> map(String codigoIndicador, List<AwesomeApiCotacaoResponse> respostaExterna) {
        return respostaExterna.stream()
                .filter(item -> item.bid() != null && item.timestamp() != null)
                .map(item -> new CotacaoExternaDTO(
                        codigoIndicador,
                        new BigDecimal(item.bid()).setScale(6, RoundingMode.HALF_UP),
                        Instant.ofEpochSecond(Long.parseLong(item.timestamp()))))
                .toList();
    }
}
