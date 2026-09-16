# Quotare

Cadastro de Indicadores e Cotações com gráfico de flutuação por período.

O repositório tem duas aplicações independentes, cada uma com seu próprio README:

- [`backend/`](backend/README.md) — Java 25 + Quarkus, MySQL 8, Traefik
- [`frontend/`](frontend/README.md) — React + Vite, servido por nginx

Para puxar cotações automaticamente de uma API externa, veja o [README da integração](backend/integracao-service/README.md).

## Subindo tudo

Pré-requisito: Docker com Docker Compose v2.20+.

```bash
cp backend/.env.example backend/.env
cp frontend/.env.example frontend/.env
docker compose up --build
```

| Endereço | O que é |
|---|---|
| http://localhost:3000 | Interface web |
| http://localhost:8080/q/swagger-ui | Swagger UI |

Logo após subir, a API pode responder `404` por alguns segundos, até o `cotacoes-service` terminar de iniciar.

Para parar: `docker compose down` (adicione `-v` para apagar os dados do banco).
