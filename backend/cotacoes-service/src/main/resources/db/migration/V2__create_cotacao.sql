CREATE TABLE cotacao (
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    indicador_id BIGINT         NOT NULL,
    valor        DECIMAL(19, 6) NOT NULL,
    data_hora    DATETIME(6)    NOT NULL COMMENT 'Sempre armazenado em UTC',
    fonte        VARCHAR(20)    NOT NULL,
    criado_em    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_cotacao_valor_positivo CHECK (valor > 0),
    CONSTRAINT ck_cotacao_fonte          CHECK (fonte IN ('LOCAL', 'EXTERNA')),
    CONSTRAINT fk_cotacao_indicador FOREIGN KEY (indicador_id) REFERENCES indicador(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_cotacao_indicador_data ON cotacao (indicador_id, data_hora DESC);
CREATE UNIQUE INDEX uk_cotacao_ponto ON cotacao (indicador_id, data_hora, fonte);
