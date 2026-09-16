package com.quotare.cotacoes.resource;

import com.quotare.cotacoes.domain.FonteDados;
import com.quotare.cotacoes.dto.AtualizarIndicadorRequest;
import com.quotare.cotacoes.dto.CriarIndicadorRequest;
import com.quotare.cotacoes.dto.IndicadorResponse;
import com.quotare.cotacoes.dto.PaginaResponse;
import com.quotare.cotacoes.service.IndicadorService;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("/api/v1/indicadores")
public class IndicadorResource {

    @Inject
    IndicadorService service;

    @POST
    public Response criar(@Valid @NotNull CriarIndicadorRequest request, @Context UriInfo uriInfo) {
        var criado = service.criar(request);
        var uri = uriInfo.getAbsolutePathBuilder().path(String.valueOf(criado.id())).build();
        return Response.created(uri).entity(criado).build();
    }

    @PUT
    @Path("/{id}")
    public IndicadorResponse atualizar(@PathParam("id") Long id, @Valid @NotNull AtualizarIndicadorRequest request) {
        return service.atualizar(id, request);
    }

    @GET
    @Path("/{id}")
    public IndicadorResponse buscarPorId(@PathParam("id") Long id) {
        return service.buscarPorId(id);
    }

    @DELETE
    @Path("/{id}")
    public Response remover(@PathParam("id") Long id) {
        service.remover(id);
        return Response.noContent().build();
    }

    @GET
    public PaginaResponse<IndicadorResponse> listar(
            @QueryParam("page") @DefaultValue("0") @PositiveOrZero int page,
            @QueryParam("size") @DefaultValue("20") @Positive @Max(100) int size,
            @QueryParam("fonte") FonteDados fonte,
            @QueryParam("ativo") Boolean ativo
    ) {
        return service.listar(page, size, fonte, ativo);
    }
}
