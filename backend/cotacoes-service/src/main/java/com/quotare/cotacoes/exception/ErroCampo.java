package com.quotare.cotacoes.exception;

public record ErroCampo(
    String campo,
    String mensagem
) {
}
