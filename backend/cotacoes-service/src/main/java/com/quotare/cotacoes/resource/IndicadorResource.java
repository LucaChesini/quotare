package com.quotare.cotacoes.resource;

import com.quotare.cotacoes.dto.IndicadorResponse;
import com.quotare.cotacoes.service.IndicadorService;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/api/v1/indicadores")
public class IndicadorResource {

    @Inject
    IndicadorService service;

    @GET
    @Path("/{id}")
    public IndicadorResponse buscarPorId(@PathParam("id") Long id) {
        return service.buscarPorId(id);
    }
}
