package com.quotare.cotacoes.exception;

public class CotacaoFonteExclusivaException extends RuntimeException {

    public CotacaoFonteExclusivaException(String codigoIndicador) {
        super("Indicador " + codigoIndicador + " (fonte=EXTERNA) não aceita cadastro manual de cotação. "
                + "As cotações são geridas pela integração automática.");
    }
}
