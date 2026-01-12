package poc.globo.globostreaming.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import poc.globo.globostreaming.messaging.producer.PaymentMessageProducer;
import poc.globo.globostreaming.model.entity.Subscription;
import poc.globo.globostreaming.model.entity.User;
import poc.globo.globostreaming.model.enums.SubscriptionPlan;
import poc.globo.globostreaming.model.enums.SubscriptionStatus;
import poc.globo.globostreaming.repository.SubscriptionRepository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionRenewalService - Testes Unitários")
class SubscriptionRenewalServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentMessageProducer messageProducer;

    @Mock
    private SubscriptionCacheService cacheService;

    @InjectMocks
    private SubscriptionRenewalService renewalService;

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
                .startDate(LocalDate.now().minusMonths(1))
                .expirationDate(LocalDate.now())
                .status(SubscriptionStatus.ATIVA)
                .autoRenew(true)
                .renewalAttempts(0)
                .build();
    }

    @Test
    @DisplayName("Deve processar renovações quando houver assinaturas que vencem hoje")
    void shouldProcessRenewalsWhenSubscriptionsExpireToday() {
        // Arrange
        List<Subscription> subscriptions = Arrays.asList(subscription);
        when(subscriptionRepository.findByStatusAndExpirationDateAndAutoRenew(
                SubscriptionStatus.ATIVA,
                LocalDate.now(),
                true
        )).thenReturn(subscriptions);

        // Act
        renewalService.processRenewals();

        // Assert
        verify(messageProducer, times(1)).sendRenewalPaymentMessage(subscription);
    }

    @Test
    @DisplayName("Não deve processar renovações quando não houver assinaturas")
    void shouldNotProcessWhenNoSubscriptions() {
        // Arrange
        when(subscriptionRepository.findByStatusAndExpirationDateAndAutoRenew(
                any(), any(), anyBoolean()
        )).thenReturn(Collections.emptyList());

        // Act
        renewalService.processRenewals();

        // Assert
        verify(messageProducer, never()).sendRenewalPaymentMessage(any());
    }

    @Test
    @DisplayName("Deve enviar múltiplas mensagens quando houver múltiplas assinaturas")
    void shouldSendMultipleMessagesForMultipleSubscriptions() {
        // Arrange
        Subscription subscription2 = Subscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(SubscriptionPlan.BASICO)
                .expirationDate(LocalDate.now())
                .status(SubscriptionStatus.ATIVA)
                .autoRenew(true)
                .renewalAttempts(0)
                .build();

        List<Subscription> subscriptions = Arrays.asList(subscription, subscription2);
        when(subscriptionRepository.findByStatusAndExpirationDateAndAutoRenew(
                any(), any(), anyBoolean()
        )).thenReturn(subscriptions);

        // Act
        renewalService.processRenewals();

        // Assert
        verify(messageProducer, times(2)).sendRenewalPaymentMessage(any());
    }

    @Test
    @DisplayName("Deve continuar processando mesmo se uma assinatura falhar")
    void shouldContinueProcessingIfOneSubscriptionFails() {
        // Arrange
        Subscription subscription2 = Subscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(SubscriptionPlan.BASICO)
                .expirationDate(LocalDate.now())
                .status(SubscriptionStatus.ATIVA)
                .autoRenew(true)
                .renewalAttempts(0)
                .build();

        List<Subscription> subscriptions = Arrays.asList(subscription, subscription2);
        when(subscriptionRepository.findByStatusAndExpirationDateAndAutoRenew(
                any(), any(), anyBoolean()
        )).thenReturn(subscriptions);

        doThrow(new RuntimeException("Erro ao enviar mensagem"))
                .when(messageProducer).sendRenewalPaymentMessage(subscription);

        // Act
        renewalService.processRenewals();

        // Assert
        verify(messageProducer, times(2)).sendRenewalPaymentMessage(any());
    }

    @Test
    @DisplayName("Deve enviar mensagem de reativação para assinatura suspensa")
    void shouldSendReactivationMessageForSuspendedSubscription() {
        // Arrange
        subscription.setStatus(SubscriptionStatus.SUSPENSA);

        // Act
        renewalService.reactivateSubscription(subscription);

        // Assert
        verify(messageProducer).sendReactivationPaymentMessage(subscription);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar reativar assinatura que não está suspensa")
    void shouldThrowExceptionWhenReactivatingNonSuspendedSubscription() {
        // Arrange
        subscription.setStatus(SubscriptionStatus.ATIVA);

        // Act & Assert
        assertThatThrownBy(() -> renewalService.reactivateSubscription(subscription))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Apenas assinaturas suspensas podem ser reativadas");

        verify(messageProducer, never()).sendReactivationPaymentMessage(any());
    }

    @Test
    @DisplayName("Deve calcular corretamente as tentativas restantes")
    void shouldCalculateRemainingAttemptsCorrectly() {
        // Arrange & Act & Assert
        subscription.setRenewalAttempts(0);
        assertThat(renewalService.getRemainingAttempts(subscription)).isEqualTo(3);

        subscription.setRenewalAttempts(1);
        assertThat(renewalService.getRemainingAttempts(subscription)).isEqualTo(2);

        subscription.setRenewalAttempts(2);
        assertThat(renewalService.getRemainingAttempts(subscription)).isEqualTo(1);

        subscription.setRenewalAttempts(3);
        assertThat(renewalService.getRemainingAttempts(subscription)).isEqualTo(0);

        subscription.setRenewalAttempts(5);
        assertThat(renewalService.getRemainingAttempts(subscription)).isEqualTo(0);
    }

    @Test
    @DisplayName("Deve processar apenas assinaturas com auto-renovação habilitada")
    void shouldProcessOnlyAutoRenewEnabledSubscriptions() {
        // Arrange
        when(subscriptionRepository.findByStatusAndExpirationDateAndAutoRenew(
                SubscriptionStatus.ATIVA,
                LocalDate.now(),
                true
        )).thenReturn(Collections.singletonList(subscription));

        // Act
        renewalService.processRenewals();

        // Assert
        verify(subscriptionRepository).findByStatusAndExpirationDateAndAutoRenew(
                SubscriptionStatus.ATIVA,
                LocalDate.now(),
                true
        );
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar reativar com falha no envio da mensagem")
    void shouldThrowExceptionWhenReactivationMessageFails() {
        // Arrange
        subscription.setStatus(SubscriptionStatus.SUSPENSA);
        doThrow(new RuntimeException("Falha ao enviar mensagem"))
                .when(messageProducer).sendReactivationPaymentMessage(subscription);

        // Act & Assert
        assertThatThrownBy(() -> renewalService.reactivateSubscription(subscription))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Falha ao enviar mensagem de reativação");
    }
}

