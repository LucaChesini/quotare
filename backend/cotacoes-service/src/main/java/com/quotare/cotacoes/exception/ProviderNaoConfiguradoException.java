package com.quotare.cotacoes.exception;

import com.quotare.cotacoes.domain.FonteDados;

public class ProviderNaoConfiguradoException extends RuntimeException {

    public ProviderNaoConfiguradoException(FonteDados fonte) {
        super("Nenhum provider configurado para a fonte " + fonte);
    }
}
