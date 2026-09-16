package com.quotare.cotacoes.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CotacaoDuplicadaExceptionMapper implements ExceptionMapper<CotacaoDuplicadaException> {

    @Override
    public Response toResponse(CotacaoDuplicadaException exception) {
        return Response.status(Response.Status.CONFLICT)
                .entity(new ProblemDetail(
                        "https://api.example.com/errors/cotacao-duplicada",
                        "Cotação duplicada",
                        Response.Status.CONFLICT.getStatusCode(),
                        exception.getMessage()
                ))
                .type("application/problem+json")
                .build();
    }
}
