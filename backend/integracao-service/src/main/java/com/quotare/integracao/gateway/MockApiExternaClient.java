package com.quotare.integracao.gateway;

import com.quotare.integracao.dto.CotacaoExternaDTO;

import io.quarkus.arc.profile.UnlessBuildProfile;

import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

@ApplicationScoped
@UnlessBuildProfile("prod")
public class MockApiExternaClient implements CotacaoExternaGateway {

    private static final BigDecimal VALOR_BASE = BigDecimal.valueOf(5.0);

    @Override
    public List<CotacaoExternaDTO> buscarSerie(String codigoIndicador, Instant inicio, Instant fim) {
        long dias = Math.max(1, ChronoUnit.DAYS.between(inicio, fim));

        return java.util.stream.LongStream.range(0, dias)
                .mapToObj(i -> {
                    Random random = new Random(seedPara(codigoIndicador, (int) i));
                    double variacao = (random.nextDouble() - 0.5) * 0.2;
                    BigDecimal valor = VALOR_BASE
                            .add(BigDecimal.valueOf(variacao))
                            .setScale(6, RoundingMode.HALF_UP);
                    Instant dataHora = fim.minus(i, ChronoUnit.DAYS);
                    return new CotacaoExternaDTO(codigoIndicador, valor, dataHora);
                })
                .toList();
    }

    private long seedPara(String codigoIndicador, int indiceDoDia) {
        return codigoIndicador.hashCode() * 31L + indiceDoDia;
    }
}
