package com.quotare.cotacoes.exception;

public class CodigoDuplicadoException extends RuntimeException {

    public CodigoDuplicadoException(String codigo) {
        super("Já existe um indicador com o código " + codigo);
    }
}
