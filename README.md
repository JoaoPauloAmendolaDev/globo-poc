# Globo Streaming

Aplicação Spring Boot rodando em Docker.

## Como rodar

### Iniciar aplicação

```bash
start.bat
```

Aguarde o build (primeira vez demora alguns minutos).

### Parar aplicação

```bash
stop.bat
```

Ou pressione `Ctrl+C` no terminal e depois:

```bash
docker compose down
```

## Acessar

- Aplicação: http://localhost:8080
- Banco de dados: localhost:5432
  - Database: `globostreaming`
  - User: `globouser`
  - Password: `globopass123`

## Comandos úteis

Ver logs:
```bash
docker compose logs -f
```

Ver logs apenas do banco:
```bash
docker compose logs -f postgres
```

Rebuild forçado:
```bash
docker compose up --build --force-recreate
```
