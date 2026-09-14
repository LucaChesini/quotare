package com.quotare.cotacoes.provider;

import com.quotare.cotacoes.domain.FonteDados;
import com.quotare.cotacoes.exception.ProviderNaoConfiguradoException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class CotacaoProviderFactory {

    @Inject
    Instance<CotacaoProvider> providers;

    public CotacaoProvider para(FonteDados fonte) {
        return providers.stream()
                .filter(p -> p.suporta(fonte))
                .findFirst()
                .orElseThrow(() -> new ProviderNaoConfiguradoException(fonte));
    }
}
