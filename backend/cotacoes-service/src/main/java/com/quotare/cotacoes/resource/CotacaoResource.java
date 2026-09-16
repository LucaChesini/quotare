package com.quotare.cotacoes.resource;

import com.quotare.cotacoes.dto.AtualizarCotacaoRequest;
import com.quotare.cotacoes.dto.CriarCotacaoRequest;
import com.quotare.cotacoes.dto.CotacaoResponse;
import com.quotare.cotacoes.dto.GranularidadeSerie;
import com.quotare.cotacoes.dto.PaginaResponse;
import com.quotare.cotacoes.dto.SerieResponse;
import com.quotare.cotacoes.service.CotacaoService;

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

import java.time.Instant;

@Path("/api/v1/cotacoes")
public class CotacaoResource {

    @Inject
    CotacaoService service;

    @POST
    public Response criar(@Valid @NotNull CriarCotacaoRequest request, @Context UriInfo uriInfo) {
        var criado = service.criar(request);
        var uri = uriInfo.getAbsolutePathBuilder().path(String.valueOf(criado.id())).build();
        return Response.created(uri).entity(criado).build();
    }

    @PUT
    @Path("/{id}")
    public CotacaoResponse atualizar(@PathParam("id") Long id, @Valid @NotNull AtualizarCotacaoRequest request) {
        return service.atualizar(id, request);
    }

    @GET
    @Path("/{id}")
    public CotacaoResponse buscarPorId(@PathParam("id") Long id) {
        return service.buscarPorId(id);
    }

    @DELETE
    @Path("/{id}")
    public Response remover(@PathParam("id") Long id) {
        service.remover(id);
        return Response.noContent().build();
    }

    @GET
    public PaginaResponse<CotacaoResponse> listar(
            @QueryParam("page") @DefaultValue("0") @PositiveOrZero int page,
            @QueryParam("size") @DefaultValue("20") @Positive @Max(100) int size,
            @QueryParam("indicadorId") Long indicadorId,
            @QueryParam("inicio") Instant inicio,
            @QueryParam("fim") Instant fim
    ) {
        return service.listar(page, size, indicadorId, inicio, fim);
    }

    @GET
    @Path("/serie")
    public SerieResponse buscarSerie(
            @QueryParam("indicadorId") @NotNull Long indicadorId,
            @QueryParam("inicio") @NotNull Instant inicio,
            @QueryParam("fim") @NotNull Instant fim,
            @QueryParam("granularidade") @DefaultValue("DIA") GranularidadeSerie granularidade
    ) {
        return service.buscarSerie(indicadorId, inicio, fim, granularidade);
    }
}
