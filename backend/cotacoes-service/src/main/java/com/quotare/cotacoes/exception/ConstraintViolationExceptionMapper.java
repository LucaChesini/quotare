package com.quotare.cotacoes.exception;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<ErroCampo> errors = exception.getConstraintViolations().stream()
                .map(this::paraErroCampo)
                .sorted(Comparator.comparing(ErroCampo::campo).thenComparing(ErroCampo::mensagem))
                .toList();

        String detail = errors.size() == 1
                ? "A requisição contém 1 campo inválido"
                : "A requisição contém " + errors.size() + " campos inválidos";

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ProblemDetail(
                        "https://api.example.com/errors/validacao",
                        "Dados inválidos",
                        Response.Status.BAD_REQUEST.getStatusCode(),
                        detail,
                        errors
                ))
                .type("application/problem+json")
                .build();
    }

    private ErroCampo paraErroCampo(ConstraintViolation<?> violation) {
        if (violacaoDoParametroDeCorpo(violation)) {
            return new ErroCampo("corpo", violation.getMessage());
        }

        String path = violation.getPropertyPath().toString();
        String campo = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
        return new ErroCampo(campo, violation.getMessage());
    }

    private boolean violacaoDoParametroDeCorpo(ConstraintViolation<?> violation) {
        List<Path.Node> nodes = new ArrayList<>();
        violation.getPropertyPath().forEach(nodes::add);

        if (nodes.size() < 2) {
            return false;
        }

        Path.Node ultimo = nodes.get(nodes.size() - 1);
        Path.Node penultimo = nodes.get(nodes.size() - 2);

        if (ultimo.getKind() != ElementKind.PARAMETER || penultimo.getKind() != ElementKind.METHOD) {
            return false;
        }

        int indiceParametro = ultimo.as(Path.ParameterNode.class).getParameterIndex();
        String nomeMetodo = penultimo.getName();
        Class<?> classeReal = classeDoResource(violation.getRootBeanClass());

        return Arrays.stream(classeReal.getDeclaredMethods())
                .filter(metodo -> metodo.getName().equals(nomeMetodo)
                        && indiceParametro < metodo.getParameterCount())
                .findFirst()
                .map(metodo -> metodo.getParameters()[indiceParametro])
                .map(parametro -> !parametro.isAnnotationPresent(QueryParam.class)
                        && !parametro.isAnnotationPresent(PathParam.class))
                .orElse(false);
    }

    private Class<?> classeDoResource(Class<?> classe) {
        Class<?> atual = classe;
        while (atual != null && !atual.isAnnotationPresent(jakarta.ws.rs.Path.class)) {
            atual = atual.getSuperclass();
        }
        return atual != null ? atual : classe;
    }
}
