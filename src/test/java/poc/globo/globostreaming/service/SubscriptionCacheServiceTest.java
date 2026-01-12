package poc.globo.globostreaming.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import poc.globo.globostreaming.model.dto.SubscriptionCacheDTO;
import poc.globo.globostreaming.model.entity.Subscription;
import poc.globo.globostreaming.model.entity.User;
import poc.globo.globostreaming.model.enums.SubscriptionPlan;
import poc.globo.globostreaming.model.enums.SubscriptionStatus;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionCacheService - Testes Unitários")
class SubscriptionCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private SubscriptionCacheService cacheService;

    private User user;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@email.com")
                .cpf("12345678901")
                .build();

        subscription = Subscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(SubscriptionPlan.PREMIUM)
                .startDate(LocalDate.now())
                .expirationDate(LocalDate.now().plusMonths(1))
                .status(SubscriptionStatus.ATIVA)
                .build();

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Deve armazenar assinatura ativa em cache com sucesso")
    void shouldCacheActiveSubscription() {
        // Act
        cacheService.cacheActiveSubscription(subscription);

        // Assert
        verify(valueOperations).set(
                eq("subscription:active:1"),
                any(SubscriptionCacheDTO.class),
                any(Duration.class)
        );
    }

    @Test
    @DisplayName("Deve retornar assinatura ativa do cache quando existir")
    void shouldReturnActiveSubscriptionFromCache() {
        // Arrange
        SubscriptionCacheDTO cachedDto = SubscriptionCacheDTO.builder()
                .subscriptionId(subscription.getId())
                .userId(1L)
                .plan(SubscriptionPlan.PREMIUM)
                .status(SubscriptionStatus.ATIVA)
                .expirationDate(LocalDate.now().plusMonths(1))
                .startDate(LocalDate.now())
                .build();

        when(valueOperations.get("subscription:active:1")).thenReturn(cachedDto);

        // Act
        Optional<SubscriptionCacheDTO> result = cacheService.getActiveSubscription(1L);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(1L);
        assertThat(result.get().getPlan()).isEqualTo(SubscriptionPlan.PREMIUM);
        assertThat(result.get().getStatus()).isEqualTo(SubscriptionStatus.ATIVA);
    }

    @Test
    @DisplayName("Deve retornar Optional vazio quando não houver cache")
    void shouldReturnEmptyWhenNoCacheExists() {
        // Arrange
        when(valueOperations.get("subscription:active:1")).thenReturn(null);

        // Act
        Optional<SubscriptionCacheDTO> result = cacheService.getActiveSubscription(1L);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Deve invalidar cache com sucesso")
    void shouldInvalidateCacheSuccessfully() {
        // Act
        cacheService.invalidateCache(1L);

        // Assert
        verify(redisTemplate).delete("subscription:active:1");
    }


    @Test
    @DisplayName("Deve armazenar assinatura CANCELADA_PENDENTE em cache")
    void shouldCacheCanceledPendingSubscription() {
        // Arrange
        subscription.setStatus(SubscriptionStatus.CANCELADA_PENDENTE);

        // Act
        cacheService.cacheActiveSubscription(subscription);

        // Assert
        verify(valueOperations).set(
                eq("subscription:active:1"),
                argThat((SubscriptionCacheDTO dto) ->
                        dto.getStatus() == SubscriptionStatus.CANCELADA_PENDENTE
                ),
                any(Duration.class)
        );
    }

    @Test
    @DisplayName("Deve retornar Optional vazio quando cache retornar tipo inválido")
    void shouldReturnEmptyWhenCacheReturnsInvalidType() {
        // Arrange
        when(valueOperations.get("subscription:active:1")).thenReturn("invalid-type");

        // Act
        Optional<SubscriptionCacheDTO> result = cacheService.getActiveSubscription(1L);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Deve construir chave de cache corretamente")
    void shouldBuildCacheKeyCorrectly() {
        // Act
        cacheService.invalidateCache(999L);

        // Assert
        verify(redisTemplate).delete("subscription:active:999");
    }
}

