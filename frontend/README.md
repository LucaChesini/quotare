# Quotare — Frontend

## Com Docker

Pré-requisito: Docker com Docker Compose v2.

```bash
cd frontend
cp .env.example .env
docker compose up --build
```

Interface em http://localhost:3000.

Sem o backend no ar, a interface carrega normalmente e exibe erro nas telas que dependem da API.

## Em modo dev

Pré-requisito: Node.js 22.

```bash
cd frontend
npm ci
npm run dev
```

Interface em http://localhost:5173. Por padrão, `/api` é repassado para o `cotacoes-service` em modo dev (`http://localhost:8081`). Para usar o backend em Docker:

```bash
env BACKEND_URL=http://localhost:8080 npm run dev
```
