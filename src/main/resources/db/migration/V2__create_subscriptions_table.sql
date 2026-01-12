CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL,
    plan VARCHAR(20) NOT NULL CHECK (plan IN ('BASICO', 'PREMIUM', 'FAMILIA')),
    start_date DATE NOT NULL,
    expiration_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVA' CHECK (status IN ('ATIVA', 'CANCELADA_PENDENTE', 'CANCELADA', 'EXPIRADA', 'SUSPENSA')),
    created_at DATE NOT NULL DEFAULT CURRENT_DATE,
    updated_at DATE,
    CONSTRAINT fk_subscription_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Index to search subscriptions by user
CREATE INDEX idx_subscriptions_user_id ON subscriptions(user_id);

-- Index to search active subscriptions
CREATE INDEX idx_subscriptions_status ON subscriptions(status);

-- Composite index to search active subscription of a user
CREATE INDEX idx_subscriptions_user_status ON subscriptions(user_id, status);

