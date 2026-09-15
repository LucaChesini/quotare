package com.quotare.integracao.resource;

import com.quotare.integracao.dto.CotacaoExternaDTO;
import com.quotare.integracao.gateway.CotacaoExternaGateway;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Path("/cotacoes-externas")
public class CotacaoExternaResource {

    @Inject
    CotacaoExternaGateway gateway;

    @GET
    @Path("/{codigo}/serie")
    public List<CotacaoExternaDTO> buscarSerie(
            @PathParam("codigo") String codigo,
            @QueryParam("inicio") String inicioParam,
            @QueryParam("fim") String fimParam) {
        Instant fim = parseOuPadrao("fim", fimParam, Instant.now());
        Instant inicio = parseOuPadrao("inicio", inicioParam, fim.minus(30, ChronoUnit.DAYS));

        if (!inicio.isBefore(fim)) {
            throw new BadRequestException("O parâmetro 'inicio' deve ser anterior a 'fim'.");
        }

        return gateway.buscarSerie(codigo, inicio, fim);
    }

    private Instant parseOuPadrao(String nomeParametro, String valor, Instant padrao) {
        if (valor == null) {
            return padrao;
        }
        try {
            return Instant.parse(valor);
        } catch (DateTimeParseException e) {
            throw new BadRequestException(
                    "Parâmetro '" + nomeParametro + "' deve estar em formato ISO-8601, ex.: 2026-08-25T14:30:00Z");
        }
    }
}
