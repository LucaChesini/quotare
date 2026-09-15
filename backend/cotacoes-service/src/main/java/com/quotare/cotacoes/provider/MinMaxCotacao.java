package com.quotare.cotacoes.provider;

import java.math.BigDecimal;

public record MinMaxCotacao(
    BigDecimal minimo,
    BigDecimal maximo
) {
}
