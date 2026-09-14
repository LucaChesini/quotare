package com.quotare.cotacoes.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ProviderNaoConfiguradoExceptionMapper implements ExceptionMapper<ProviderNaoConfiguradoException> {

    @Override
    public Response toResponse(ProviderNaoConfiguradoException exception) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ProblemDetail(
                        "https://api.example.com/errors/provider-nao-configurado",
                        "Provider não configurado",
                        Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                        exception.getMessage()
                ))
                .type("application/problem+json")
                .build();
    }
}
