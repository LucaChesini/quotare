package com.quotare.cotacoes.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class IntervaloInvalidoExceptionMapper implements ExceptionMapper<IntervaloInvalidoException> {

    @Override
    public Response toResponse(IntervaloInvalidoException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ProblemDetail(
                        "https://api.example.com/errors/intervalo-invalido",
                        "Intervalo inválido",
                        Response.Status.BAD_REQUEST.getStatusCode(),
                        exception.getMessage()
                ))
                .type("application/problem+json")
                .build();
    }
}
