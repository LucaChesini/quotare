CREATE TABLE indicador (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    codigo        VARCHAR(20)  NOT NULL,
    nome          VARCHAR(120) NOT NULL,
    fonte         VARCHAR(20)  NOT NULL,
    ativo         BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_indicador_codigo UNIQUE (codigo),
    CONSTRAINT ck_indicador_fonte CHECK (fonte IN ('LOCAL', 'EXTERNA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
