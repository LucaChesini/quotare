package com.quotare.cotacoes.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CodigoDuplicadoExceptionMapper implements ExceptionMapper<CodigoDuplicadoException> {

    @Override
    public Response toResponse(CodigoDuplicadoException exception) {
        return Response.status(Response.Status.CONFLICT)
                .entity(new ProblemDetail(
                        "https://api.example.com/errors/codigo-duplicado",
                        "Código duplicado",
                        Response.Status.CONFLICT.getStatusCode(),
                        exception.getMessage()
                ))
                .type("application/problem+json")
                .build();
    }
}
