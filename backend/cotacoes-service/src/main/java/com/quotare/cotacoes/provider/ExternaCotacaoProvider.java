package com.quotare.cotacoes.provider;

import com.quotare.cotacoes.domain.FonteDados;
import com.quotare.cotacoes.domain.Indicador;
import com.quotare.cotacoes.dto.GranularidadeSerie;
import com.quotare.cotacoes.dto.PontoResponse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ExternaCotacaoProvider implements CotacaoProvider {

    @Inject
    CotacaoSerieRepository serieRepository;

    @Inject
    IndicadorRepository indicadorRepository;

    @Override
    public List<CotacaoDTO> buscarSerie(String codigoIndicador, Instant inicio, Instant fim, GranularidadeSerie granularidade) {
        Indicador indicador = serieRepository.buscarIndicadorPorCodigo(codigoIndicador);

        List<PontoResponse> pontos = serieRepository.buscarPontos(indicador.id, inicio, fim, granularidade);

        return pontos.stream()
                .map(ponto -> new CotacaoDTO(ponto.v(), ponto.t()))
                .toList();
    }

    @Override
    public Optional<CotacaoDTO> buscarUltima(String codigoIndicador) {
        Indicador indicador = serieRepository.buscarIndicadorPorCodigo(codigoIndicador);

        return serieRepository.buscarUltimaCotacao(indicador.id)
                .map(cotacao -> new CotacaoDTO(cotacao.valor, cotacao.dataHora));
    }

    @Override
    public List<IndicadorDTO> listarIndicadoresDisponiveis() {
        return indicadorRepository.listarExternosAtivos()
                .stream()
                .map(indicador -> new IndicadorDTO(indicador.codigo, indicador.nome))
                .toList();
    }

    @Override
    public boolean suporta(FonteDados fonte) {
        return fonte == FonteDados.EXTERNA;
    }
}
