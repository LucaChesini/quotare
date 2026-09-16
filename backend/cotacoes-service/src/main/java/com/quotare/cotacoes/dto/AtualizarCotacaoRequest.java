package com.quotare.cotacoes.dto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

public record AtualizarCotacaoRequest(

        @NotNull(message = "O indicador da cotação é obrigatório")
        Long indicadorId,

        @NotNull(message = "O valor da cotação é obrigatório")
        @Positive(message = "O valor da cotação deve ser maior que zero")
        @Digits(integer = 13, fraction = 6, message = "O valor da cotação deve ter no máximo 13 dígitos inteiros e 6 casas decimais")
        BigDecimal valor,

        @NotNull(message = "A data/hora da cotação é obrigatória")
        @PastOrPresent(message = "A data/hora da cotação não pode estar no futuro")
        Instant dataHora
) {
}
