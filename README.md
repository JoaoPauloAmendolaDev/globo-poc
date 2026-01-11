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

Ver logs da aplicação (incluindo Flyway):
```bash
docker compose logs -f app
```

Ver logs do Flyway especificamente:
```bash
docker compose logs app | findstr /i "flyway"
```

Rebuild forçado:
```bash
docker compose up --build --force-recreate
```

## Migrations do Flyway

**O Flyway roda automaticamente dentro do container Docker da aplicação** quando ela inicia.

### Configuração

O Flyway está configurado para:
- ✅ Rodar automaticamente no startup da aplicação (dentro do Docker)
- ✅ Criar baseline se necessário (`baseline-on-migrate=true`)
- ✅ Validar migrations antes de aplicar
- ✅ Logs em DEBUG para máxima visibilidade

