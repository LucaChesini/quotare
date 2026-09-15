package com.quotare.integracao.gateway;

import com.quotare.integracao.client.ApiExternaClient;
import com.quotare.integracao.client.AwesomeApiCotacaoResponse;
import com.quotare.integracao.client.ExternalQuoteMapper;
import com.quotare.integracao.dto.CotacaoExternaDTO;

import io.quarkus.arc.profile.IfBuildProfile;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@ApplicationScoped
@IfBuildProfile("prod")
public class AwesomeApiCotacaoExternaGateway implements CotacaoExternaGateway {

    @Inject
    @RestClient
    ApiExternaClient client;

    @Inject
    ExternalQuoteMapper mapper;

    @Override
    public List<CotacaoExternaDTO> buscarSerie(String codigoIndicador, Instant inicio, Instant fim) {
        String par = codigoIndicador.toUpperCase() + "-BRL";
        int dias = (int) Math.max(1, ChronoUnit.DAYS.between(inicio, fim));
        List<AwesomeApiCotacaoResponse> resposta = client.buscarDiario(par, dias);
        return mapper.map(codigoIndicador, resposta);
    }
}
