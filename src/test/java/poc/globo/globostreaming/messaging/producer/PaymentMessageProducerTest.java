package poc.globo.globostreaming.messaging.producer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import poc.globo.globostreaming.config.RabbitMQConfig;
import poc.globo.globostreaming.model.dto.PaymentProcessingMessage;
import poc.globo.globostreaming.model.dto.PaymentResultMessage;
import poc.globo.globostreaming.model.entity.Subscription;
import poc.globo.globostreaming.model.entity.User;
import poc.globo.globostreaming.model.enums.SubscriptionPlan;
import poc.globo.globostreaming.model.enums.SubscriptionStatus;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentMessageProducer - Testes Unitários")
class PaymentMessageProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RabbitMQConfig rabbitMQConfig;

    @InjectMocks
    private PaymentMessageProducer messageProducer;

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
                .renewalAttempts(0)
                .build();

        lenient().when(rabbitMQConfig.getPaymentExchange()).thenReturn("payment.exchange");
        lenient().when(rabbitMQConfig.getPaymentProcessingRoutingKey()).thenReturn("payment.process");
        lenient().when(rabbitMQConfig.getPaymentResultRoutingKey()).thenReturn("payment.result");
    }

    @Test
    @DisplayName("Deve enviar mensagem de renovação com dados corretos")
    void shouldSendRenewalMessageWithCorrectData() {
        // Arrange
        ArgumentCaptor<PaymentProcessingMessage> messageCaptor = ArgumentCaptor.forClass(PaymentProcessingMessage.class);

        // Act
        messageProducer.sendRenewalPaymentMessage(subscription);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                eq("payment.exchange"),
                eq("payment.process"),
                messageCaptor.capture()
        );

        PaymentProcessingMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getSubscriptionId()).isEqualTo(subscription.getId());
        assertThat(sentMessage.getUserId()).isEqualTo(1L);
        assertThat(sentMessage.getPlan()).isEqualTo(SubscriptionPlan.PREMIUM);
        assertThat(sentMessage.getAmountInCents()).isEqualTo(3990L);
        assertThat(sentMessage.getAttemptNumber()).isEqualTo(0);
        assertThat(sentMessage.getEventType()).isEqualTo("RENEWAL");
        assertThat(sentMessage.getCorrelationId()).isNotNull();
        assertThat(sentMessage.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve enviar mensagem de renovação com número de tentativa correto")
    void shouldSendRenewalMessageWithCorrectAttemptNumber() {
        // Arrange
        subscription.setRenewalAttempts(2);
        ArgumentCaptor<PaymentProcessingMessage> messageCaptor = ArgumentCaptor.forClass(PaymentProcessingMessage.class);

        // Act
        messageProducer.sendRenewalPaymentMessage(subscription);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                any(String.class),
                any(String.class),
                messageCaptor.capture()
        );

        assertThat(messageCaptor.getValue().getAttemptNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve calcular valor correto para plano BASICO")
    void shouldCalculateCorrectAmountForBasicPlan() {
        // Arrange
        subscription.setPlan(SubscriptionPlan.BASICO);
        ArgumentCaptor<PaymentProcessingMessage> messageCaptor = ArgumentCaptor.forClass(PaymentProcessingMessage.class);

        // Act
        messageProducer.sendRenewalPaymentMessage(subscription);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                any(String.class),
                any(String.class),
                messageCaptor.capture()
        );

        assertThat(messageCaptor.getValue().getAmountInCents()).isEqualTo(1990L);
    }

    @Test
    @DisplayName("Deve calcular valor correto para plano FAMILIA")
    void shouldCalculateCorrectAmountForFamilyPlan() {
        // Arrange
        subscription.setPlan(SubscriptionPlan.FAMILIA);
        ArgumentCaptor<PaymentProcessingMessage> messageCaptor = ArgumentCaptor.forClass(PaymentProcessingMessage.class);

        // Act
        messageProducer.sendRenewalPaymentMessage(subscription);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                any(String.class),
                any(String.class),
                messageCaptor.capture()
        );

        assertThat(messageCaptor.getValue().getAmountInCents()).isEqualTo(5990L);
    }

    @Test
    @DisplayName("Deve enviar mensagem de reativação com dados corretos")
    void shouldSendReactivationMessageWithCorrectData() {
        // Arrange
        subscription.setStatus(SubscriptionStatus.SUSPENSA);
        ArgumentCaptor<PaymentProcessingMessage> messageCaptor = ArgumentCaptor.forClass(PaymentProcessingMessage.class);

        // Act
        messageProducer.sendReactivationPaymentMessage(subscription);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                eq("payment.exchange"),
                eq("payment.process"),
                messageCaptor.capture()
        );

        PaymentProcessingMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getEventType()).isEqualTo("REACTIVATION");
        assertThat(sentMessage.getAttemptNumber()).isEqualTo(0);
        assertThat(sentMessage.getSubscriptionId()).isEqualTo(subscription.getId());
    }

    @Test
    @DisplayName("Deve enviar resultado de pagamento com sucesso")
    void shouldSendPaymentResultSuccessfully() {
        // Arrange
        PaymentResultMessage result = PaymentResultMessage.builder()
                .subscriptionId(subscription.getId())
                .userId(1L)
                .success(true)
                .message("Pagamento processado")
                .build();

        // Act
        messageProducer.sendPaymentResult(result);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                eq("payment.exchange"),
                eq("payment.result"),
                eq(result)
        );
    }

    @Test
    @DisplayName("Deve lançar exceção quando falhar ao enviar mensagem de renovação")
    void shouldThrowExceptionWhenSendRenewalFails() {
        // Arrange
        doThrow(new RuntimeException("Erro no RabbitMQ"))
                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));

        // Act & Assert
        assertThatThrownBy(() -> messageProducer.sendRenewalPaymentMessage(subscription))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha ao enviar mensagem de pagamento");
    }

    @Test
    @DisplayName("Deve lançar exceção quando falhar ao enviar mensagem de reativação")
    void shouldThrowExceptionWhenSendReactivationFails() {
        // Arrange
        doThrow(new RuntimeException("Erro no RabbitMQ"))
                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));

        // Act & Assert
        assertThatThrownBy(() -> messageProducer.sendReactivationPaymentMessage(subscription))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha ao enviar mensagem de pagamento");
    }

    @Test
    @DisplayName("Não deve lançar exceção quando falhar ao enviar resultado de pagamento")
    void shouldNotThrowExceptionWhenSendResultFails() {
        // Arrange
        PaymentResultMessage result = PaymentResultMessage.builder()
                .subscriptionId(subscription.getId())
                .success(false)
                .build();

        doThrow(new RuntimeException("Erro no RabbitMQ"))
                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));

        // Act & Assert - Não deve lançar exceção
        messageProducer.sendPaymentResult(result);

        verify(rabbitTemplate).convertAndSend(any(String.class), any(String.class), eq(result));
    }

    @Test
    @DisplayName("Deve gerar CorrelationId único para cada mensagem")
    void shouldGenerateUniqueCorrelationIdForEachMessage() {
        // Arrange
        ArgumentCaptor<PaymentProcessingMessage> messageCaptor = ArgumentCaptor.forClass(PaymentProcessingMessage.class);

        // Act
        messageProducer.sendRenewalPaymentMessage(subscription);
        messageProducer.sendRenewalPaymentMessage(subscription);

        // Assert
        verify(rabbitTemplate, times(2)).convertAndSend(
                any(String.class),
                any(String.class),
                messageCaptor.capture()
        );

        String firstCorrelationId = messageCaptor.getAllValues().get(0).getCorrelationId();
        String secondCorrelationId = messageCaptor.getAllValues().get(1).getCorrelationId();

        assertThat(firstCorrelationId).isNotEqualTo(secondCorrelationId);
    }
}

