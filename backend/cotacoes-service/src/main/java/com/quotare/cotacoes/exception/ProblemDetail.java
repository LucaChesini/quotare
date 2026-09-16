package com.quotare.cotacoes.exception;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemDetail(String type, String title, int status, String detail, List<ErroCampo> errors) {

    public ProblemDetail(String type, String title, int status, String detail) {
        this(type, title, status, detail, null);
    }
}
