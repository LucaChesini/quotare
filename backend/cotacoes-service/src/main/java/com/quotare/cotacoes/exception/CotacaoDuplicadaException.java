package com.quotare.cotacoes.exception;

import com.quotare.cotacoes.domain.FonteDados;

import java.time.Instant;

public class CotacaoDuplicadaException extends RuntimeException {

    public CotacaoDuplicadaException(String codigoIndicador, Instant dataHora, FonteDados fonte) {
        super("Já existe uma cotação para o indicador " + codigoIndicador
                + " em " + dataHora + " pela fonte " + fonte);
    }
}
