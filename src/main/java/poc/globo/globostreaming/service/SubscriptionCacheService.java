package poc.globo.globostreaming.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import poc.globo.globostreaming.model.dto.SubscriptionCacheDTO;
import poc.globo.globostreaming.model.entity.Subscription;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionCacheService {

    private static final String CACHE_PREFIX = "subscription:active:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);

    private final RedisTemplate<String, Object> redisTemplate;


    public Optional<SubscriptionCacheDTO> getActiveSubscription(Long userId) {
        try {
            String key = buildKey(userId);
            SubscriptionCacheDTO subscriptionCached = (SubscriptionCacheDTO) redisTemplate.opsForValue().get(key);

            if (subscriptionCached != null) {
                log.debug("Cache HIT - Assinatura ativa encontrada no cache para userId: {}", userId);

                if (subscriptionCached.isActive()) {
                    return Optional.of(subscriptionCached);
                } else {
                    log.debug("Assinatura em cache expirou para userId: {}. Invalidando...", userId);
                    invalidateCache(userId);
                    return Optional.empty();
                }
            }

            log.debug("Cache MISS - Assinatura não encontrada no cache para userId: {}", userId);
            return Optional.empty();

        } catch (Exception e) {
            log.error("Erro ao buscar assinatura do cache para userId: {}", userId, e);
            return Optional.empty();
        }
    }


    public void cacheActiveSubscription(Subscription subscription) {
        try {
            if (subscription == null || subscription.getUser() == null) {
                log.warn("Tentativa de cache de assinatura nula ou sem usuário");
                return;
            }

            Long userId = subscription.getUser().getId();
            String key = buildKey(userId);

            SubscriptionCacheDTO cacheDTO = SubscriptionCacheDTO.builder()
                    .subscriptionId(subscription.getId())
                    .userId(userId)
                    .plan(subscription.getPlan())
                    .status(subscription.getStatus())
                    .expirationDate(subscription.getExpirationDate())
                    .startDate(subscription.getStartDate())
                    .build();

            redisTemplate.opsForValue().set(key, cacheDTO, DEFAULT_TTL);
            log.debug("Assinatura armazenada em cache para userId: {} com TTL de {}", userId, DEFAULT_TTL);

        } catch (Exception e) {
            log.error("Erro ao armazenar assinatura no cache para subscriptionId: {}",
                    subscription.getId(), e);
        }
    }


    public void invalidateCache(Long userId) {
        try {
            String key = buildKey(userId);
            Boolean deleted = redisTemplate.delete(key);

            if (Boolean.TRUE.equals(deleted)) {
                log.debug("Cache invalidado para userId: {}", userId);
            } else {
                log.debug("Nenhum cache encontrado para invalidar para userId: {}", userId);
            }

        } catch (Exception e) {
            log.error("Erro ao invalidar cache para userId: {}", userId, e);
        }
    }

    public boolean hasActiveSubscriptionInCache(Long userId) {
        try {
            String key = buildKey(userId);
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);

        } catch (Exception e) {
            log.error("Erro ao verificar existência de cache para userId: {}", userId, e);
            return false;
        }
    }

    public void clearAllCache() {
        try {
            var keys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cache de assinaturas limpo. Total de chaves removidas: {}", keys.size());
            }
        } catch (Exception e) {
            log.error("Erro ao limpar todo o cache de assinaturas", e);
        }
    }

    private String buildKey(Long userId) {
        return CACHE_PREFIX + userId;
    }
}

