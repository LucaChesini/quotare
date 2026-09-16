package com.quotare.cotacoes.exception;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.PropertyBindingException;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.USER)
public class MismatchedInputExceptionMapper implements ExceptionMapper<MismatchedInputException> {

    @Override
    public Response toResponse(MismatchedInputException exception) {
        String campo = nomeDoCampo(exception);
        String detail = mensagemDetalhada(exception, campo);
        List<ErroCampo> errors = campo != null ? List.of(new ErroCampo(campo, detail)) : null;

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ProblemDetail(
                        "https://api.example.com/errors/campo-mal-formatado",
                        "Campo mal formatado",
                        Response.Status.BAD_REQUEST.getStatusCode(),
                        detail,
                        errors
                ))
                .type("application/problem+json")
                .build();
    }

    private String nomeDoCampo(MismatchedInputException exception) {
        if (exception.getPath().isEmpty()) {
            return null;
        }
        JsonMappingException.Reference referencia = exception.getPath().get(exception.getPath().size() - 1);
        return referencia.getFieldName();
    }

    private String mensagemDetalhada(MismatchedInputException exception, String campo) {
        String nomeCampo = campo != null ? "'" + campo + "'" : "Um dos campos";

        if (exception instanceof PropertyBindingException) {
            return "O campo " + nomeCampo + " não é reconhecido";
        }

        Class<?> tipoAlvo = exception.getTargetType();

        if (tipoAlvo != null && tipoAlvo.isEnum()) {
            String valoresAceitos = Arrays.stream(tipoAlvo.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            return "O campo " + nomeCampo + " aceita apenas os valores: " + valoresAceitos;
        }

        if (tipoAlvo != null && (Number.class.isAssignableFrom(tipoAlvo)
                || tipoAlvo == int.class || tipoAlvo == long.class || tipoAlvo == double.class)) {
            return "O campo " + nomeCampo + " deve ser um número válido";
        }

        if (tipoAlvo != null && java.time.temporal.Temporal.class.isAssignableFrom(tipoAlvo)) {
            return "O campo " + nomeCampo + " deve ser uma data/hora válida no formato ISO-8601";
        }

        return "O campo " + nomeCampo + " possui um valor com formato inválido";
    }
}
