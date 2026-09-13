package com.quotare.cotacoes.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CotacaoFonteExclusivaExceptionMapper implements ExceptionMapper<CotacaoFonteExclusivaException> {

    @Override
    public Response toResponse(CotacaoFonteExclusivaException exception) {
        return Response.status(Response.Status.CONFLICT)
                .entity(new ProblemDetail(
                        "https://api.example.com/errors/fonte-exclusiva",
                        "Cotação gerida por integração automática",
                        Response.Status.CONFLICT.getStatusCode(),
                        exception.getMessage()
                ))
                .type("application/problem+json")
                .build();
    }
}
