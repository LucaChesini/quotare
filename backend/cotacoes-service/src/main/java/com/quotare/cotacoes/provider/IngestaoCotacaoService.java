package com.quotare.cotacoes.provider;

import com.quotare.cotacoes.client.CotacaoExternaDTO;
import com.quotare.cotacoes.domain.Cotacao;
import com.quotare.cotacoes.domain.FonteDados;
import com.quotare.cotacoes.domain.Indicador;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class IngestaoCotacaoService {

    private static final String UPSERT_SQL = """
            INSERT INTO cotacao (indicador_id, valor, data_hora, fonte, criado_em)
            VALUES (:indicadorId, :valor, :dataHora, 'EXTERNA', CURRENT_TIMESTAMP) AS novo
            ON DUPLICATE KEY UPDATE valor = novo.valor
            """;

    @Inject
    CotacaoSerieRepository serieRepository;

    @ConfigProperty(name = "ingestao.janela-inicial")
    Duration janelaInicial;

    public Instant inicioDaJanela(Indicador indicador) {
        return serieRepository.buscarUltimaCotacaoPorFonte(indicador.id, FonteDados.EXTERNA)
                .map(cotacao -> cotacao.dataHora)
                .orElseGet(() -> Instant.now().minus(janelaInicial));
    }

    @Transactional
    public void upsertDeIngestao(Long indicadorId, List<CotacaoExternaDTO> pontos) {
        for (CotacaoExternaDTO ponto : pontos) {
            Cotacao.getEntityManager()
                    .createNativeQuery(UPSERT_SQL)
                    .setParameter("indicadorId", indicadorId)
                    .setParameter("valor", ponto.valor())
                    .setParameter("dataHora", ponto.dataHora())
                    .executeUpdate();
        }
    }
}
