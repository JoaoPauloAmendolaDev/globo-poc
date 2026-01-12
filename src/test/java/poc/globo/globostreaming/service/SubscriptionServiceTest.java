package poc.globo.globostreaming.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import poc.globo.globostreaming.exception.ActiveSubscriptionExistsException;
import poc.globo.globostreaming.exception.SubscriptionNotFoundException;
import poc.globo.globostreaming.model.dto.CreateSubscriptionRequestDTO;
import poc.globo.globostreaming.model.dto.SubscriptionCacheDTO;
import poc.globo.globostreaming.model.dto.SubscriptionResponseDTO;
import poc.globo.globostreaming.model.entity.Subscription;
import poc.globo.globostreaming.model.entity.User;
import poc.globo.globostreaming.model.enums.SubscriptionPlan;
import poc.globo.globostreaming.model.enums.SubscriptionStatus;
import poc.globo.globostreaming.repository.SubscriptionRepository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionService - Testes Unitários")
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionCacheService cacheService;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private User testUser;
    private Subscription testSubscription;
    private CreateSubscriptionRequestDTO createRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("João Silva")
                .email("joao@email.com")
                .cpf("12345678901")
                .passwordHash("hashedPassword")
                .build();

        testSubscription = Subscription.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .plan(SubscriptionPlan.PREMIUM)
                .startDate(LocalDate.now())
                .expirationDate(LocalDate.now().plusMonths(1))
                .status(SubscriptionStatus.ATIVA)
                .autoRenew(true)
                .renewalAttempts(0)
                .build();

        createRequest = new CreateSubscriptionRequestDTO(SubscriptionPlan.PREMIUM);
    }

    @Test
    @DisplayName("Deve criar assinatura com sucesso")
    void shouldCreateSubscriptionSuccessfully() {
        // Arrange
        when(cacheService.getActiveSubscription(1L)).thenReturn(Optional.empty());
        when(subscriptionRepository.existsByUserIdAndStatus(eq(1L), any())).thenReturn(false);
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

        // Act
        SubscriptionResponseDTO response = subscriptionService.createSubscription(testUser, createRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.plan()).isEqualTo(SubscriptionPlan.PREMIUM);
        assertThat(response.status()).isEqualTo(SubscriptionStatus.ATIVA);

        verify(subscriptionRepository).save(any(Subscription.class));
        verify(cacheService).cacheActiveSubscription(any(Subscription.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário já tem assinatura ativa no cache")
    void shouldThrowExceptionWhenUserHasActiveSubscriptionInCache() {
        // Arrange
        SubscriptionCacheDTO cachedSubscription = SubscriptionCacheDTO.builder()
                .subscriptionId(UUID.randomUUID())
                .userId(1L)
                .status(SubscriptionStatus.ATIVA)
                .build();

        when(cacheService.getActiveSubscription(1L)).thenReturn(Optional.of(cachedSubscription));

        // Act & Assert
        assertThatThrownBy(() -> subscriptionService.createSubscription(testUser, createRequest))
                .isInstanceOf(ActiveSubscriptionExistsException.class);

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário já tem assinatura ativa no banco")
    void shouldThrowExceptionWhenUserHasActiveSubscriptionInDatabase() {
        // Arrange
        when(cacheService.getActiveSubscription(1L)).thenReturn(Optional.empty());
        when(subscriptionRepository.existsByUserIdAndStatus(1L, SubscriptionStatus.ATIVA)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> subscriptionService.createSubscription(testUser, createRequest))
                .isInstanceOf(ActiveSubscriptionExistsException.class);

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve buscar assinatura ativa do cache primeiro")
    void shouldGetActiveSubscriptionFromCacheFirst() {
        // Arrange
        SubscriptionCacheDTO cachedSubscription = SubscriptionCacheDTO.builder()
                .subscriptionId(testSubscription.getId())
                .userId(1L)
                .plan(SubscriptionPlan.PREMIUM)
                .status(SubscriptionStatus.ATIVA)
                .startDate(LocalDate.now())
                .expirationDate(LocalDate.now().plusMonths(1))
                .build();

        when(cacheService.getActiveSubscription(1L)).thenReturn(Optional.of(cachedSubscription));

        // Act
        SubscriptionResponseDTO response = subscriptionService.getActiveSubscription(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.plan()).isEqualTo(SubscriptionPlan.PREMIUM);
        verify(subscriptionRepository, never()).findByUserIdAndStatus(any(), any());
    }

    @Test
    @DisplayName("Deve buscar assinatura ativa do banco quando cache está vazio")
    void shouldGetActiveSubscriptionFromDatabaseWhenCacheMiss() {
        // Arrange
        when(cacheService.getActiveSubscription(1L)).thenReturn(Optional.empty());
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ATIVA))
                .thenReturn(Optional.of(testSubscription));

        // Act
        SubscriptionResponseDTO response = subscriptionService.getActiveSubscription(1L);

        // Assert
        assertThat(response).isNotNull();
        verify(subscriptionRepository).findByUserIdAndStatus(1L, SubscriptionStatus.ATIVA);
        verify(cacheService).cacheActiveSubscription(testSubscription);
    }

    @Test
    @DisplayName("Deve lançar exceção quando não existe assinatura ativa")
    void shouldThrowExceptionWhenNoActiveSubscription() {
        // Arrange
        when(cacheService.getActiveSubscription(1L)).thenReturn(Optional.empty());
        when(subscriptionRepository.findByUserIdAndStatus(eq(1L), any()))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> subscriptionService.getActiveSubscription(1L))
                .isInstanceOf(SubscriptionNotFoundException.class);
    }

    @Test
    @DisplayName("Deve listar todas as assinaturas do usuário")
    void shouldListAllUserSubscriptions() {
        // Arrange
        Subscription oldSubscription = Subscription.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .plan(SubscriptionPlan.BASICO)
                .startDate(LocalDate.now().minusMonths(3))
                .expirationDate(LocalDate.now().minusMonths(2))
                .status(SubscriptionStatus.EXPIRADA)
                .build();

        List<Subscription> subscriptions = Arrays.asList(testSubscription, oldSubscription);
        when(subscriptionRepository.findAllByUserId(1L)).thenReturn(subscriptions);

        // Act
        List<SubscriptionResponseDTO> response = subscriptionService.listUserSubscriptions(1L);

        // Assert
        assertThat(response).hasSize(2);
        verify(subscriptionRepository).findAllByUserId(1L);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando usuário não tem assinaturas")
    void shouldReturnEmptyListWhenNoSubscriptions() {
        // Arrange
        when(subscriptionRepository.findAllByUserId(1L)).thenReturn(List.of());

        // Act
        List<SubscriptionResponseDTO> response = subscriptionService.listUserSubscriptions(1L);

        // Assert
        assertThat(response).isEmpty();
    }

    @Test
    @DisplayName("Deve cancelar assinatura com sucesso (CANCELADA_PENDENTE)")
    void shouldCancelSubscriptionSuccessfully() {
        // Arrange
        when(subscriptionRepository.findById(testSubscription.getId()))
                .thenReturn(Optional.of(testSubscription));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SubscriptionResponseDTO response = subscriptionService.cancelSubscription(
                testSubscription.getId(), 1L);

        // Assert
        assertThat(response.status()).isEqualTo(SubscriptionStatus.CANCELADA_PENDENTE);

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SubscriptionStatus.CANCELADA_PENDENTE);
        assertThat(captor.getValue().getCanceledAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve lançar exceção quando assinatura não encontrada ao cancelar")
    void shouldThrowExceptionWhenSubscriptionNotFoundOnCancel() {
        // Arrange
        UUID randomId = UUID.randomUUID();
        when(subscriptionRepository.findById(randomId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> subscriptionService.cancelSubscription(randomId, 1L))
                .isInstanceOf(SubscriptionNotFoundException.class);
    }

    @Test
    @DisplayName("Deve lançar exceção ao cancelar assinatura de outro usuário")
    void shouldThrowExceptionWhenCancellingAnotherUsersSubscription() {
        // Arrange
        when(subscriptionRepository.findById(testSubscription.getId()))
                .thenReturn(Optional.of(testSubscription));

        // Act & Assert
        assertThatThrownBy(() -> subscriptionService.cancelSubscription(
                testSubscription.getId(), 999L))
                .isInstanceOf(SubscriptionNotFoundException.class);
    }

    @Test
    @DisplayName("Deve lançar exceção ao cancelar assinatura não ativa")
    void shouldThrowExceptionWhenCancellingNonActiveSubscription() {
        // Arrange
        testSubscription.setStatus(SubscriptionStatus.EXPIRADA);
        when(subscriptionRepository.findById(testSubscription.getId()))
                .thenReturn(Optional.of(testSubscription));

        // Act & Assert
        assertThatThrownBy(() -> subscriptionService.cancelSubscription(
                testSubscription.getId(), 1L))
                .isInstanceOf(ActiveSubscriptionExistsException.class);
    }

    @Test
    @DisplayName("Deve ativar renovação automática")
    void shouldEnableAutoRenew() {
        // Arrange
        testSubscription.setAutoRenew(false);
        when(subscriptionRepository.findById(testSubscription.getId()))
                .thenReturn(Optional.of(testSubscription));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SubscriptionResponseDTO response = subscriptionService.toggleAutoRenew(
                testSubscription.getId(), 1L, true);

        // Assert
        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getAutoRenew()).isTrue();
    }

    @Test
    @DisplayName("Deve desativar renovação automática")
    void shouldDisableAutoRenew() {
        // Arrange
        testSubscription.setAutoRenew(true);
        when(subscriptionRepository.findById(testSubscription.getId()))
                .thenReturn(Optional.of(testSubscription));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        subscriptionService.toggleAutoRenew(testSubscription.getId(), 1L, false);

        // Assert
        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getAutoRenew()).isFalse();
    }

    @Test
    @DisplayName("Deve verificar que usuário tem acesso com assinatura ativa no cache")
    void shouldVerifyUserHasAccessWithActiveSubscriptionInCache() {
        // Arrange
        SubscriptionCacheDTO cachedSubscription = SubscriptionCacheDTO.builder()
                .subscriptionId(UUID.randomUUID())
                .userId(1L)
                .status(SubscriptionStatus.ATIVA)
                .expirationDate(LocalDate.now().plusDays(10))
                .build();

        when(cacheService.getActiveSubscription(1L)).thenReturn(Optional.of(cachedSubscription));

        // Act
        boolean hasAccess = subscriptionService.userHasAccess(1L);

        // Assert
        assertThat(hasAccess).isTrue();
    }

    @Test
    @DisplayName("Deve verificar que usuário não tem acesso sem assinatura")
    void shouldVerifyUserHasNoAccessWithoutSubscription() {
        // Arrange
        when(cacheService.getActiveSubscription(1L)).thenReturn(Optional.empty());
        when(subscriptionRepository.findByUserIdAndStatus(eq(1L), any()))
                .thenReturn(Optional.empty());

        // Act
        boolean hasAccess = subscriptionService.userHasAccess(1L);

        // Assert
        assertThat(hasAccess).isFalse();
    }

    @Test
    @DisplayName("Deve processar assinaturas expiradas")
    void shouldProcessExpiredSubscriptions() {
        // Arrange
        Subscription expiredSubscription = Subscription.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .plan(SubscriptionPlan.PREMIUM)
                .startDate(LocalDate.now().minusMonths(2))
                .expirationDate(LocalDate.now().minusDays(1))
                .status(SubscriptionStatus.ATIVA)
                .build();

        when(subscriptionRepository.findByStatusAndExpirationDateBefore(
                eq(SubscriptionStatus.ATIVA), any(LocalDate.class)))
                .thenReturn(List.of(expiredSubscription));
        when(subscriptionRepository.findByStatusAndExpirationDateBefore(
                eq(SubscriptionStatus.CANCELADA_PENDENTE), any(LocalDate.class)))
                .thenReturn(List.of());

        // Act
        subscriptionService.processExpiredSubscriptions();

        // Assert
        verify(subscriptionRepository).save(argThat(sub ->
                sub.getStatus() == SubscriptionStatus.EXPIRADA));
        verify(cacheService).invalidateCache(1L);
    }

    @Test
    @DisplayName("Deve criar assinatura com data de expiração correta (1 mês)")
    void shouldCreateSubscriptionWithCorrectExpirationDate() {
        // Arrange
        when(cacheService.getActiveSubscription(1L)).thenReturn(Optional.empty());
        when(subscriptionRepository.existsByUserIdAndStatus(eq(1L), any())).thenReturn(false);
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        subscriptionService.createSubscription(testUser, createRequest);

        // Assert
        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());

        Subscription saved = captor.getValue();
        assertThat(saved.getStartDate()).isEqualTo(LocalDate.now());
        assertThat(saved.getExpirationDate()).isEqualTo(LocalDate.now().plusMonths(1));
    }

    @Test
    @DisplayName("Deve atualizar cache ao cancelar assinatura para CANCELADA_PENDENTE")
    void shouldUpdateCacheOnCancelToPending() {
        // Arrange
        when(subscriptionRepository.findById(testSubscription.getId()))
                .thenReturn(Optional.of(testSubscription));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        subscriptionService.cancelSubscription(testSubscription.getId(), 1L);

        // Assert
        verify(cacheService).cacheActiveSubscription(any(Subscription.class));
    }
}

