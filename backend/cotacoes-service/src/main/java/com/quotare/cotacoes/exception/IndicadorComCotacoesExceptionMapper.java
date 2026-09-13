package com.quotare.cotacoes.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class IndicadorComCotacoesExceptionMapper implements ExceptionMapper<IndicadorComCotacoesException> {

    @Override
    public Response toResponse(IndicadorComCotacoesException exception) {
        return Response.status(Response.Status.CONFLICT)
                .entity(new ProblemDetail(
                        "https://api.example.com/errors/indicador-com-cotacoes",
                        "Indicador possui cotações associadas",
                        Response.Status.CONFLICT.getStatusCode(),
                        exception.getMessage()
                ))
                .type("application/problem+json")
                .build();
    }
}
