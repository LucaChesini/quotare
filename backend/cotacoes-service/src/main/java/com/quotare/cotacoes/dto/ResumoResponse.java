package com.quotare.cotacoes.dto;

import java.math.BigDecimal;

public record ResumoResponse(
        BigDecimal minimo,
        BigDecimal maximo,
        BigDecimal variacaoPercentual
) {
}
