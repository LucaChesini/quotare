package com.quotare.integracao.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.cache.CacheResult;

import java.util.List;

@RegisterRestClient(configKey = "api-externa")
@Path("/json/daily")
public interface ApiExternaClient {

    @GET
    @Path("/{par}/{dias}")
    @Retry(maxRetries = 3, delay = 500)
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5)
    @Fallback(fallbackMethod = "fallbackVazio")
    @CacheResult(cacheName = "cotacao-externa")
    List<AwesomeApiCotacaoResponse> buscarDiario(@PathParam("par") String par, @PathParam("dias") int dias);

    default List<AwesomeApiCotacaoResponse> fallbackVazio(String par, int dias) {
        return List.of();
    }
}
