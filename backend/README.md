# Quotare — Backend

## Com Docker

Pré-requisito: Docker com Docker Compose v2.

```bash
cd backend
cp .env.example .env
docker compose up --build
```

| Endereço | O que é |
|---|---|
| http://localhost:8080/api/v1/... | API (via gateway) |
| http://localhost:8080/q/swagger-ui | Swagger UI |
| `127.0.0.1:3307` | MySQL |

Logo após subir, a API pode responder `404` por alguns segundos, até o `cotacoes-service` terminar de iniciar.

Se o frontend já estiver rodando, o Compose avisa que a rede `app-net` não foi criada por este projeto. É só um aviso, a rede é reaproveitada.

Para parar: `docker compose down` (adicione `-v` para apagar os dados do banco).

As variáveis de ambiente estão descritas em `.env.example`.

## Em modo dev

Pré-requisitos: Java 25 e Docker (para o MySQL).

Em dev, o `integracao-service` usa dados simulados no lugar da API externa.

```bash
# terminal 1 — banco (usa backend/.env)
cd backend
docker compose up mysql

# terminal 2
cd backend/integracao-service
./mvnw quarkus:dev

# terminal 3
cd backend/cotacoes-service
cp .env.example .env
./mvnw quarkus:dev
```

- `cotacoes-service`: http://localhost:8081
- `integracao-service`: http://localhost:8082

## Testes

Exigem Docker rodando (o `cotacoes-service` sobe um MySQL temporário para os testes).

```bash
cd backend/cotacoes-service && ./mvnw test
cd backend/integracao-service && ./mvnw test
```
