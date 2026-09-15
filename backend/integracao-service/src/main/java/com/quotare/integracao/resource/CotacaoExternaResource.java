package com.quotare.integracao.resource;

import com.quotare.integracao.dto.CotacaoExternaDTO;
import com.quotare.integracao.gateway.CotacaoExternaGateway;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import java.util.List;

@Path("/cotacoes-externas")
public class CotacaoExternaResource {

    @Inject
    CotacaoExternaGateway gateway;

    @GET
    @Path("/{codigo}/serie")
    public List<CotacaoExternaDTO> buscarSerie(
            @PathParam("codigo") String codigo,
            @QueryParam("dias") @DefaultValue("30") int dias) {
        if (dias <= 0) {
            throw new BadRequestException("O parâmetro 'dias' deve ser maior que zero.");
        }
        return gateway.buscarSerie(codigo, dias);
    }
}
