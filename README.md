# Globo Streaming

POC (proof of concept) criado na intenção de simular um sistema simples de Streaming,
é possível rodar todo o sistema com docker, entretanto, podemos rodar apenas o banco e rodar a aplicação localmente também.

## 🚀 Funcionalidades Principais

### ✅ Autenticação e Autorização
- Sistema de autenticação com JWT
- Registro e login de usuários
- Proteção de endpoints com Spring Security

### ✅ Gerenciamento de Assinaturas
- Criação de assinaturas (BASICO, PREMIUM, FAMILIA)
- Visualização de assinatura ativa
- Cancelamento de assinaturas
- Ativação/Desativação de auto-renovação

### ✅ Renovação Automática de Assinaturas
- ⚡ Processamento automático diário (00:00)
- 🔄 Até 3 tentativas de renovação
- 💳 Integração com serviço de pagamento simulado
- ⚠️ Suspensão automática após 3 falhas
- 📊 Logs detalhados de cada renovação

### ✅ Processamento Assíncrono com RabbitMQ
- 📨 Mensageria para processamento de pagamentos
- 🔄 Retry automático (3 tentativas)
- 💀 Dead Letter Queue para falhas
- 📊 Management UI para monitoramento
- ⚡ Performance e escalabilidade

### ✅ Cache com Redis
- 🚀 Cache de assinaturas ativas para performance
- ⏱️ TTL de 1 hora (configurável)
- 🔄 Invalidação automática em mudanças
- 📈 Redução de 95%+ em consultas ao banco

### ✅ Cancelamento Inteligente
- ⏰ Uso até o fim do ciclo após cancelamento
- 🔄 Status CANCELADA_PENDENTE
- 📅 Conversão automática após expiração

## Como rodar

### Iniciar aplicação pelo docker

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

## Acessar Aplicação

- **Aplicação**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs (JSON)**: http://localhost:8080/v3/api-docs
- **PostgreSQL**: localhost:5433
  - Database: `globostreaming`
  - User: `globouser`
  - Password: `globopass123`
- **Redis**: localhost:6379
- **RabbitMQ Management UI**: http://localhost:15672
  - User: `globouser`
  - Password: `globopass123`
  - Queues: `payment.processing.queue`, `payment.result.queue`, `payment.dlq`

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
