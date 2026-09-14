package com.quotare.integracao.client;

public record AwesomeApiCotacaoResponse(
    String code,
    String codein,
    String bid,
    String timestamp
) {
}
