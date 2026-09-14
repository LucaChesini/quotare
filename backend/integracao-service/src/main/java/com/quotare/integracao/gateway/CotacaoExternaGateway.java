package com.quotare.integracao.gateway;

import com.quotare.integracao.dto.CotacaoExternaDTO;

import java.util.List;

public interface CotacaoExternaGateway {

    List<CotacaoExternaDTO> buscarSerie(String codigoIndicador, int dias);
}
