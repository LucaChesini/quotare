package com.quotare.cotacoes.job;

import com.quotare.cotacoes.client.CotacaoExternaDTO;
import com.quotare.cotacoes.client.IntegracaoClient;
import com.quotare.cotacoes.domain.Cotacao;
import com.quotare.cotacoes.domain.FonteDados;
import com.quotare.cotacoes.domain.Indicador;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@QuarkusTest
@DisplayName("IngestaoCotacaoJob")
class IngestaoCotacaoJobTest {

    @Inject
    IngestaoCotacaoJob job;

    @InjectMock
    @RestClient
    IntegracaoClient integracaoMock;

    @BeforeEach
    void limparBancoEMock() {
        QuarkusTransaction.requiringNew().run(() -> {
            Cotacao.deleteAll();
            Indicador.deleteAll();
        });
        Mockito.reset(integracaoMock);
    }

    private Indicador persistirIndicador(String codigo, String nome, FonteDados fonte, boolean ativo) {
        return QuarkusTransaction.requiringNew().call(() -> {
            var indicador = new Indicador();
            indicador.codigo = codigo;
            indicador.nome = nome;
            indicador.fonte = fonte;
            indicador.ativo = ativo;
            indicador.persist();
            return indicador;
        });
    }

    private void persistirCotacao(Indicador indicador, FonteDados fonte, BigDecimal valor, Instant dataHora) {
        QuarkusTransaction.requiringNew().run(() -> {
            var cotacao = new Cotacao();
            cotacao.indicador = indicador;
            cotacao.valor = valor;
            cotacao.dataHora = dataHora;
            cotacao.fonte = fonte;
            cotacao.persist();
        });
    }

    private long contarCotacoesExternas(Long indicadorId) {
        return QuarkusTransaction.requiringNew().call(() ->
                Cotacao.count("indicador.id = :indicadorId and fonte = :fonte",
                        Map.of("indicadorId", indicadorId, "fonte", FonteDados.EXTERNA))
        );
    }

    @Test
    @DisplayName("reprocessar a mesma janela não duplica as cotações gravadas")
    void reprocessarMesmaJanelaNaoDuplica() {
        var indicador = persistirIndicador("ING_A", "Indicador A", FonteDados.EXTERNA, true);

        var pontos = List.of(
                new CotacaoExternaDTO("ING_A", new BigDecimal("5.10"), Instant.parse("2024-06-01T09:00:00Z")),
                new CotacaoExternaDTO("ING_A", new BigDecimal("5.20"), Instant.parse("2024-06-01T10:00:00Z"))
        );

        Mockito.when(integracaoMock.buscarSerie(eq("ING_A"), any(), any())).thenReturn(pontos);

        job.executar();
        long totalAposPrimeiraRodada = contarCotacoesExternas(indicador.id);

        job.executar();
        long totalAposSegundaRodada = contarCotacoesExternas(indicador.id);

        assertEquals(2, totalAposPrimeiraRodada);
        assertEquals(totalAposPrimeiraRodada, totalAposSegundaRodada);
    }

    @Test
    @DisplayName("indicador LOCAL ou EXTERNA inativo é ignorado pelo job")
    void indicadorLocalOuInativoEhIgnorado() {
        var externoAtivo = persistirIndicador("EXT_ATIVO", "Externo ativo", FonteDados.EXTERNA, true);
        persistirIndicador("LOCAL_ATIVO", "Local ativo", FonteDados.LOCAL, true);
        persistirIndicador("EXT_INATIVO", "Externo inativo", FonteDados.EXTERNA, false);

        Mockito.when(integracaoMock.buscarSerie(eq("EXT_ATIVO"), any(), any()))
                .thenReturn(List.of(new CotacaoExternaDTO("EXT_ATIVO", new BigDecimal("1.00"), Instant.parse("2024-06-01T09:00:00Z"))));

        job.executar();

        Mockito.verify(integracaoMock, Mockito.times(1)).buscarSerie(eq("EXT_ATIVO"), any(), any());
        Mockito.verify(integracaoMock, Mockito.never()).buscarSerie(eq("LOCAL_ATIVO"), any(), any());
        Mockito.verify(integracaoMock, Mockito.never()).buscarSerie(eq("EXT_INATIVO"), any(), any());

        assertEquals(1, contarCotacoesExternas(externoAtivo.id));
    }

    @Test
    @DisplayName("falha em um indicador não impede a ingestão dos demais")
    void falhaEmUmIndicadorNaoImpedeOsDemais() {
        persistirIndicador("IND_FALHA", "Indicador com falha", FonteDados.EXTERNA, true);
        var indicadorOk = persistirIndicador("IND_OK", "Indicador OK", FonteDados.EXTERNA, true);

        Mockito.when(integracaoMock.buscarSerie(eq("IND_FALHA"), any(), any()))
                .thenThrow(new RuntimeException("simulando falha de rede"));

        var pontosOk = List.of(new CotacaoExternaDTO("IND_OK", new BigDecimal("3.50"), Instant.parse("2024-06-01T09:00:00Z")));
        Mockito.when(integracaoMock.buscarSerie(eq("IND_OK"), any(), any())).thenReturn(pontosOk);

        assertDoesNotThrow(() -> job.executar());

        assertEquals(1, contarCotacoesExternas(indicadorOk.id));
    }

    @Test
    @DisplayName("a janela da rodada começa no último ponto EXTERNA já persistido")
    void janelaComecaNoUltimoPontoPersistido() {
        var indicador = persistirIndicador("IND_D", "Indicador com histórico", FonteDados.EXTERNA, true);
        var dataHoraExistente = Instant.parse("2024-06-01T10:00:00Z").truncatedTo(ChronoUnit.MICROS);
        persistirCotacao(indicador, FonteDados.EXTERNA, new BigDecimal("2.00"), dataHoraExistente);

        Mockito.when(integracaoMock.buscarSerie(eq("IND_D"), any(), any())).thenReturn(List.of());

        job.executar();

        ArgumentCaptor<Instant> inicioCapturado = ArgumentCaptor.forClass(Instant.class);
        Mockito.verify(integracaoMock).buscarSerie(eq("IND_D"), inicioCapturado.capture(), any());

        assertEquals(dataHoraExistente, inicioCapturado.getValue());
    }
}
