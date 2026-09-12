package com.quotare.cotacoes.resource;

import com.quotare.cotacoes.dto.IndicadorResponse;
import com.quotare.cotacoes.dto.PaginaResponse;
import com.quotare.cotacoes.service.IndicadorService;

import jakarta.inject.Inject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

@Path("/api/v1/indicadores")
public class IndicadorResource {

    @Inject
    IndicadorService service;

    @GET
    @Path("/{id}")
    public IndicadorResponse buscarPorId(@PathParam("id") Long id) {
        return service.buscarPorId(id);
    }

    @GET
    public PaginaResponse<IndicadorResponse> listar(
            @QueryParam("page") @DefaultValue("0") @PositiveOrZero int page,
            @QueryParam("size") @DefaultValue("20") @Positive @Max(100) int size
    ) {
        return service.listar(page, size);
    }
}
