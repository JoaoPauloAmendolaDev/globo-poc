-- Adiciona campo para data de cancelamento
ALTER TABLE subscriptions
ADD COLUMN canceled_at DATE;

-- Adicionar comentário explicativo
COMMENT ON COLUMN subscriptions.canceled_at IS 'Data em que a assinatura foi cancelada pelo usuário';

