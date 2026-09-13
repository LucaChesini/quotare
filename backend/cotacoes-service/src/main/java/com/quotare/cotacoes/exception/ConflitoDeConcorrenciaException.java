package com.quotare.cotacoes.exception;

public class ConflitoDeConcorrenciaException extends RuntimeException {

    public ConflitoDeConcorrenciaException() {
        super("Não foi possível salvar devido a uma alteração concorrente. Tente novamente.");
    }
}
