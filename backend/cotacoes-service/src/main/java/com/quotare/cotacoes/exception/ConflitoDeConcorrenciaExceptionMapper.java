package com.quotare.cotacoes.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ConflitoDeConcorrenciaExceptionMapper implements ExceptionMapper<ConflitoDeConcorrenciaException> {

    @Override
    public Response toResponse(ConflitoDeConcorrenciaException exception) {
        return Response.status(Response.Status.CONFLICT)
                .entity(new ProblemDetail(
                        "https://api.example.com/errors/conflito-de-concorrencia",
                        "Conflito de concorrência",
                        Response.Status.CONFLICT.getStatusCode(),
                        exception.getMessage()
                ))
                .type("application/problem+json")
                .build();
    }
}
