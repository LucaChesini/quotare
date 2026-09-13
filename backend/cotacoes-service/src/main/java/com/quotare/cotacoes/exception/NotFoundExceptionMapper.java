package com.quotare.cotacoes.exception;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    @Override
    public Response toResponse(NotFoundException exception) {
        if (exception.getCause() instanceof ParametroInvalidoException parametroInvalido) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ProblemDetail(
                            "https://api.example.com/errors/parametro-invalido",
                            "Parâmetro inválido",
                            Response.Status.BAD_REQUEST.getStatusCode(),
                            parametroInvalido.getMessage()
                    ))
                    .type("application/problem+json")
                    .build();
        }

        if (exception.getCause() instanceof NumberFormatException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ProblemDetail(
                            "https://api.example.com/errors/parametro-invalido",
                            "Parâmetro inválido",
                            Response.Status.BAD_REQUEST.getStatusCode(),
                            "Um dos parâmetros numéricos da requisição possui um valor inválido."
                    ))
                    .type("application/problem+json")
                    .build();
        }

        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ProblemDetail(
                        "https://api.example.com/errors/recurso-nao-encontrado",
                        "Recurso não encontrado",
                        Response.Status.NOT_FOUND.getStatusCode(),
                        "O recurso solicitado não foi encontrado."
                ))
                .type("application/problem+json")
                .build();
    }
}
