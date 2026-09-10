package com.quotare.cotacoes.dto;

import com.quotare.cotacoes.domain.FonteDados;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AtualizarIndicadorRequest(

        @NotBlank(message = "O código do indicador é obrigatório")
        @Size(max = 20, message = "O código do indicador deve ter no máximo 20 caracteres")
        @Pattern(
                regexp = "^[A-Za-z0-9]+$",
                message = "O código do indicador deve conter apenas letras e números, sem espaços ou símbolos"
        )
        String codigo,

        @NotBlank(message = "O nome do indicador é obrigatório")
        @Size(max = 120, message = "O nome do indicador deve ter no máximo 120 caracteres")
        String nome,

        @NotNull(message = "A fonte de dados do indicador é obrigatória")
        FonteDados fonte,

        @NotNull(message = "O campo 'ativo' é obrigatório")
        Boolean ativo
) {
}
