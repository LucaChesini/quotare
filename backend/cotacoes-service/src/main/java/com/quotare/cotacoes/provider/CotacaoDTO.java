package com.quotare.cotacoes.provider;

import java.math.BigDecimal;
import java.time.Instant;

public record CotacaoDTO(BigDecimal valor, Instant dataHora) {
}
