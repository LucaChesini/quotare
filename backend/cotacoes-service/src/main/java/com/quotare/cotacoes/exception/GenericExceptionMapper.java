package com.quotare.cotacoes.exception;

import com.fasterxml.jackson.core.JsonParseException;

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
            if (webApplicationException.getResponse().getStatus() == Response.Status.BAD_REQUEST.getStatusCode()
                    && exception.getCause() instanceof JsonParseException) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ProblemDetail(
                                "https://api.example.com/errors/json-invalido",
                                "JSON inválido",
                                Response.Status.BAD_REQUEST.getStatusCode(),
                                "O corpo da requisição não contém um JSON válido."
                        ))
                        .type("application/problem+json")
                        .build();
            }
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
