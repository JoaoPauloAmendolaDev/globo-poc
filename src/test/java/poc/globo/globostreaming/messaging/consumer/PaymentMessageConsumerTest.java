package poc.globo.globostreaming.messaging.consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import poc.globo.globostreaming.messaging.producer.PaymentMessageProducer;
import poc.globo.globostreaming.model.dto.PaymentProcessingMessage;
import poc.globo.globostreaming.model.dto.PaymentResultMessage;
import poc.globo.globostreaming.model.entity.Subscription;
import poc.globo.globostreaming.model.entity.User;
import poc.globo.globostreaming.model.enums.SubscriptionPlan;
import poc.globo.globostreaming.model.enums.SubscriptionStatus;
import poc.globo.globostreaming.repository.SubscriptionRepository;
import poc.globo.globostreaming.service.PaymentService;
import poc.globo.globostreaming.service.SubscriptionCacheService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentMessageConsumer - Testes Unitários")
class PaymentMessageConsumerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionCacheService cacheService;

    @Mock
    private PaymentMessageProducer messageProducer;

    @InjectMocks
    private PaymentMessageConsumer messageConsumer;

    private User user;
    private Subscription subscription;
    private PaymentProcessingMessage paymentMessage;

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
                .renewalAttempts(0)
                .build();

        paymentMessage = PaymentProcessingMessage.builder()
                .subscriptionId(subscription.getId())
                .userId(1L)
                .plan(SubscriptionPlan.PREMIUM)
                .amountInCents(3990L)
                .attemptNumber(0)
                .eventType("RENEWAL")
                .correlationId(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Deve processar pagamento com sucesso e renovar assinatura")
    void shouldProcessPaymentSuccessfullyAndRenewSubscription() {
        // Arrange
        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
        when(paymentService.processRenewalPayment(subscription)).thenReturn(true);

        // Act
        messageConsumer.processPayment(paymentMessage);

        // Assert
        verify(subscriptionRepository).save(argThat(sub ->
                sub.getExpirationDate().isEqual(LocalDate.now().plusMonths(1)) &&
                sub.getRenewalAttempts() == 0 &&
                sub.getStatus() == SubscriptionStatus.ATIVA
        ));
        verify(cacheService).cacheActiveSubscription(any(Subscription.class));
        verify(messageProducer).sendPaymentResult(argThat(result ->
                result.isSuccess() && result.getCorrelationId().equals(paymentMessage.getCorrelationId())
        ));
    }

    @Test
    @DisplayName("Deve incrementar tentativas quando pagamento falhar")
    void shouldIncrementAttemptsWhenPaymentFails() {
        // Arrange
        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
        when(paymentService.processRenewalPayment(subscription)).thenReturn(false);

        // Act
        messageConsumer.processPayment(paymentMessage);

        // Assert
        verify(subscriptionRepository).save(argThat(sub ->
                sub.getRenewalAttempts() == 1
        ));
        verify(messageProducer).sendPaymentResult(argThat(result ->
                !result.isSuccess() &&
                result.getMessage().contains("Falha no pagamento")
        ));
    }

    @Test
    @DisplayName("Deve suspender assinatura após 3 tentativas falhadas")
    void shouldSuspendSubscriptionAfterThreeFailedAttempts() {
        // Arrange
        subscription.setRenewalAttempts(2);
        paymentMessage.setAttemptNumber(2);

        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
        when(paymentService.processRenewalPayment(subscription)).thenReturn(false);

        // Act
        messageConsumer.processPayment(paymentMessage);

        // Assert
        verify(subscriptionRepository).save(argThat(sub ->
                sub.getStatus() == SubscriptionStatus.SUSPENSA &&
                sub.getRenewalAttempts() == 3
        ));
        verify(cacheService).invalidateCache(1L);
        verify(messageProducer).sendPaymentResult(argThat(result ->
                !result.isSuccess() &&
                result.getErrorCode().equals("MAX_ATTEMPTS_REACHED")
        ));
    }

    @Test
    @DisplayName("Deve processar mensagem de resultado sem erros")
    void shouldProcessResultMessageWithoutErrors() {
        // Arrange
        PaymentResultMessage result = PaymentResultMessage.builder()
                .subscriptionId(subscription.getId())
                .userId(1L)
                .success(true)
                .message("Teste")
                .build();

        // Act
        messageConsumer.processPaymentResult(result);

        // Assert - Não deve lançar exceção
        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    @DisplayName("Deve lidar com assinatura não encontrada")
    void shouldHandleSubscriptionNotFound() {
        // Arrange
        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.empty());

        // Act
        messageConsumer.processPayment(paymentMessage);

        // Assert
        verify(subscriptionRepository, never()).save(any());
        verify(cacheService, never()).cacheActiveSubscription(any());
    }

    @Test
    @DisplayName("Deve resetar tentativas após pagamento bem-sucedido")
    void shouldResetAttemptsAfterSuccessfulPayment() {
        // Arrange
        subscription.setRenewalAttempts(2);
        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
        when(paymentService.processRenewalPayment(subscription)).thenReturn(true);

        // Act
        messageConsumer.processPayment(paymentMessage);

        // Assert
        verify(subscriptionRepository).save(argThat(sub ->
                sub.getRenewalAttempts() == 0
        ));
    }

    @Test
    @DisplayName("Deve estender expiração por 1 mês após sucesso")
    void shouldExtendExpirationByOneMonthAfterSuccess() {
        // Arrange
        LocalDate originalExpiration = LocalDate.now();
        subscription.setExpirationDate(originalExpiration);

        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
        when(paymentService.processRenewalPayment(subscription)).thenReturn(true);

        // Act
        messageConsumer.processPayment(paymentMessage);

        // Assert
        verify(subscriptionRepository).save(argThat(sub ->
                sub.getExpirationDate().isEqual(originalExpiration.plusMonths(1))
        ));
    }

    @Test
    @DisplayName("Deve manter status ATIVA após renovação bem-sucedida")
    void shouldKeepActiveStatusAfterSuccessfulRenewal() {
        // Arrange
        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
        when(paymentService.processRenewalPayment(subscription)).thenReturn(true);

        // Act
        messageConsumer.processPayment(paymentMessage);

        // Assert
        verify(subscriptionRepository).save(argThat(sub ->
                sub.getStatus() == SubscriptionStatus.ATIVA
        ));
    }

    @Test
    @DisplayName("Deve enviar resultado com correlationId correto")
    void shouldSendResultWithCorrectCorrelationId() {
        // Arrange
        String correlationId = "test-correlation-id";
        paymentMessage.setCorrelationId(correlationId);

        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
        when(paymentService.processRenewalPayment(subscription)).thenReturn(true);

        ArgumentCaptor<PaymentResultMessage> resultCaptor = ArgumentCaptor.forClass(PaymentResultMessage.class);

        // Act
        messageConsumer.processPayment(paymentMessage);

        // Assert
        verify(messageProducer).sendPaymentResult(resultCaptor.capture());
        assertThat(resultCaptor.getValue().getCorrelationId()).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Deve processar pagamento de reativação")
    void shouldProcessReactivationPayment() {
        // Arrange
        subscription.setStatus(SubscriptionStatus.SUSPENSA);
        paymentMessage.setEventType("REACTIVATION");

        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
        when(paymentService.processRenewalPayment(subscription)).thenReturn(true);

        // Act
        messageConsumer.processPayment(paymentMessage);

        // Assert
        verify(subscriptionRepository).save(argThat(sub ->
                sub.getStatus() == SubscriptionStatus.ATIVA
        ));
    }

    @Test
    @DisplayName("Deve invalidar cache apenas quando suspender assinatura")
    void shouldInvalidateCacheOnlyWhenSuspending() {
        // Arrange
        subscription.setRenewalAttempts(2);

        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
        when(paymentService.processRenewalPayment(subscription)).thenReturn(false);

        // Act
        messageConsumer.processPayment(paymentMessage);

        // Assert
        verify(cacheService).invalidateCache(1L);
        verify(cacheService, never()).cacheActiveSubscription(any());
    }

    @Test
    @DisplayName("Não deve invalidar cache quando ainda houver tentativas")
    void shouldNotInvalidateCacheWhenAttemptsRemain() {
        // Arrange
        subscription.setRenewalAttempts(0);

        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
        when(paymentService.processRenewalPayment(subscription)).thenReturn(false);

        // Act
        messageConsumer.processPayment(paymentMessage);

        // Assert
        verify(cacheService, never()).invalidateCache(any());
    }
}

