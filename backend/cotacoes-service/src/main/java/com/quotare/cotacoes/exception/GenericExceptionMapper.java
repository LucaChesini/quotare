package com.quotare.cotacoes.exception;

import io.quarkus.logging.Log;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof WebApplicationException webApplicationException) {
            return webApplicationException.getResponse();
        }

        Log.error("Erro não tratado", exception);

        return Response.status(500)
                .entity(new ProblemDetail(
                        "https://api.example.com/errors/erro-interno",
                        "Erro interno",
                        500,
                        "Ocorreu um erro inesperado. Tente novamente mais tarde."
                ))
                .type("application/problem+json")
                .build();
    }
}
