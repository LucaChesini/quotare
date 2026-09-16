# integracao-service

Busca cotações de moedas na [AwesomeAPI](https://awesomeapi.com.br/) para os indicadores de fonte **Externa**.

## Como funciona

A cada rodada, o `cotacoes-service` pede ao `integracao-service` as cotações de cada indicador **Externo** e **ativo**, e grava o resultado no banco. Quem consulta a AwesomeAPI é o `integracao-service`.

- Endpoint consultado: `GET https://economia.awesomeapi.com.br/json/daily/{CODIGO}-BRL/{dias}`, sem chave de API.
- O código do indicador é o código da moeda. O serviço completa o par com `-BRL`, então os valores são em reais.
- A primeira rodada de um indicador busca os últimos 30 dias (`INGESTAO_JANELA_INICIAL`). As seguintes continuam a partir da última cotação gravada.

Em modo dev (`./mvnw quarkus:dev`), a AwesomeAPI não é chamada: o serviço devolve dados simulados. A API real só é usada no Docker.

## Puxando cotações

Basta cadastrar um indicador com fonte **Externa**. Pela tela de Indicadores:

| Código | Nome | Fonte | Ativo |
|---|---|---|---|
| `USD` | Dólar Americano | Externa | sim |
| `EUR` | Euro | Externa | sim |

As cotações aparecem depois da próxima rodada. Outras moedas com par em reais estão listadas em https://economia.awesomeapi.com.br/json/available (os pares terminados em `-BRL`).

## Periodicidade

Controlada por `INGESTAO_CRON` em `backend/.env`. O padrão é a cada 15 minutos:

```
INGESTAO_CRON=0 0/15 * * * ?
```

Para rodar a cada 1 minuto:

```
INGESTAO_CRON=0 * * * * ?
```

Depois de alterar, recrie só o `cotacoes-service`, na mesma pasta em que o Compose foi iniciado:

```bash
docker compose up -d cotacoes-service
```

Use `INGESTAO_CRON=off` para desligar a ingestão.

## Observações

- Cotações de indicadores externos não podem ser criadas, editadas ou excluídas manualmente.
