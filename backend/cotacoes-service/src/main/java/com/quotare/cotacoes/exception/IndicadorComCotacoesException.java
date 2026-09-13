package com.quotare.cotacoes.exception;

public class IndicadorComCotacoesException extends RuntimeException {

    public IndicadorComCotacoesException() {
        super("Não é possível remover um indicador com cotações associadas");
    }
}
