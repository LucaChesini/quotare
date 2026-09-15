package com.quotare.cotacoes.client;

import java.time.Instant;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "integracao-service")
@Path("/cotacoes-externas")
public interface IntegracaoClient {

    @GET
    @Path("/{codigo}/serie")
    List<CotacaoExternaDTO> buscarSerie(@PathParam("codigo") String codigo, @QueryParam("inicio") Instant inicio, @QueryParam("fim") Instant fim);
}
