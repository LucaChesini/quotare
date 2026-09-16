package com.quotare.cotacoes.exception;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("GenericExceptionMapper")
class GenericExceptionMapperTest {

    private final GenericExceptionMapper mapper = new GenericExceptionMapper();

    @Test
    @DisplayName("converte qualquer exceção em um 500 com detail genérico, sem vazar a mensagem original")
    void convertePara500ComDetailGenerico() {
        var exception = new RuntimeException("mensagem interna sensível");

        Response response = mapper.toResponse(exception);

        assertEquals(500, response.getStatus(), "status HTTP deveria ser 500");
        assertEquals(MediaType.valueOf("application/problem+json"), response.getMediaType(),
                "content-type deveria ser application/problem+json");

        var corpo = (ProblemDetail) response.getEntity();

        assertEquals("https://api.example.com/errors/erro-interno", corpo.type(), "type deveria identificar o erro interno");
        assertEquals("Erro interno", corpo.title(), "title deveria ser fixo para este tipo de erro");
        assertEquals(500, corpo.status(), "status do corpo deveria ser igual ao status HTTP");
        assertTrue(corpo.detail().length() > 0, "detail não deveria ser vazio");
        assertFalse(corpo.detail().contains("mensagem interna sensível"),
                "detail nunca deveria ecoar a mensagem original da exceção, que pode conter dados sensíveis");
    }

    @Test
    @DisplayName("repassa a Response original de uma WebApplicationException, sem reembrulhar em ProblemDetail 500")
    void repassaResponseOriginalDeWebApplicationException() {
        var notFoundException = new NotFoundException();

        Response response = mapper.toResponse(notFoundException);

        assertEquals(404, response.getStatus(), "status HTTP deveria ser o 404 original da exceção, não 500");
        assertNull(response.getEntity(), "entity deveria continuar nula, sem passar pelo fluxo de log+500 genérico");
    }

    @Test
    @DisplayName("repassa a Response original de uma WebApplicationException 400 sem causa JsonParseException")
    void repassaResponseOriginalDeWebApplicationException400SemCausaJsonParseException() {
        var badRequestSemCausa = new WebApplicationException(Response.status(400).build());

        Response response = mapper.toResponse(badRequestSemCausa);

        assertEquals(400, response.getStatus(), "status HTTP deveria continuar 400");
        assertNull(response.getEntity(), "entity deveria continuar nula, sem virar json-invalido");
    }

    @Test
    @DisplayName("repassa a Response original de uma WebApplicationException 400 com causa que não é JsonParseException")
    void repassaResponseOriginalDeWebApplicationException400ComOutraCausa() {
        var badRequestComOutraCausa = new WebApplicationException(
                new IllegalArgumentException("causa não relacionada a JSON"),
                Response.status(400).build());

        Response response = mapper.toResponse(badRequestComOutraCausa);

        assertEquals(400, response.getStatus(), "status HTTP deveria continuar 400");
        assertNull(response.getEntity(), "entity deveria continuar nula, sem virar json-invalido");
    }
}
