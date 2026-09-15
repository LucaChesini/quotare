package com.quotare.cotacoes.job;

import com.quotare.cotacoes.client.CotacaoExternaDTO;
import com.quotare.cotacoes.client.IntegracaoClient;
import com.quotare.cotacoes.domain.Indicador;
import com.quotare.cotacoes.provider.IndicadorRepository;
import com.quotare.cotacoes.provider.IngestaoCotacaoService;

import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class IngestaoCotacaoJob {

    @Inject
    IndicadorRepository indicadores;

    @Inject
    IngestaoCotacaoService ingestao;

    @Inject
    @RestClient
    IntegracaoClient integracao;

    @Scheduled(cron = "{ingestao.cron}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void executar() {
        List<Indicador> indicadoresExternos = indicadores.listarExternosAtivos();
        Log.infof("Iniciando rodada de ingestao para %d indicador(es)", indicadoresExternos.size());

        int totalDePontosGravados = 0;
        for (Indicador indicador : indicadoresExternos) {
            try {
                Instant inicio = ingestao.inicioDaJanela(indicador);
                List<CotacaoExternaDTO> pontos = integracao.buscarSerie(indicador.codigo, inicio, Instant.now());
                ingestao.upsertDeIngestao(indicador.id, pontos);

                totalDePontosGravados += pontos.size();
                Log.infof("Indicador %s: %d ponto(s) gravado(s)", indicador.codigo, pontos.size());
            } catch (Exception e) {
                Log.errorf(e, "Falha na ingestão do indicador %s", indicador.codigo);
            }
        }

        Log.infof("Rodada de ingestao concluida: %d indicador(es) processado(s), %d ponto(s) gravado(s) no total",
                indicadoresExternos.size(), totalDePontosGravados);
    }
}
