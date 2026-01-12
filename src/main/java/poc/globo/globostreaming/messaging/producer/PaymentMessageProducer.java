package poc.globo.globostreaming.messaging.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import poc.globo.globostreaming.config.RabbitMQConfig;
import poc.globo.globostreaming.model.dto.PaymentProcessingMessage;
import poc.globo.globostreaming.model.dto.PaymentResultMessage;
import poc.globo.globostreaming.model.entity.Subscription;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentMessageProducer {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQConfig rabbitMQConfig;

    public void sendRenewalPaymentMessage(Subscription subscription) {
        try {
            String correlationId = UUID.randomUUID().toString();

            PaymentProcessingMessage message = PaymentProcessingMessage.builder()
                    .subscriptionId(subscription.getId())
                    .userId(subscription.getUser().getId())
                    .plan(subscription.getPlan())
                    .amountInCents(calculateAmount(subscription))
                    .attemptNumber(subscription.getRenewalAttempts())
                    .eventType("RENEWAL")
                    .correlationId(correlationId)
                    .createdAt(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    rabbitMQConfig.getPaymentExchange(),
                    rabbitMQConfig.getPaymentProcessingRoutingKey(),
                    message
            );

            log.info("Mensagem de renovação enviada para fila. SubscriptionId: {}, CorrelationId: {}, Tentativa: {}",
                    subscription.getId(), correlationId, subscription.getRenewalAttempts() + 1);

        } catch (Exception e) {
            log.error("Erro ao enviar mensagem de renovação para fila. SubscriptionId: {}",
                    subscription.getId(), e);
            throw new RuntimeException("Falha ao enviar mensagem de pagamento", e);
        }
    }

    public void sendReactivationPaymentMessage(Subscription subscription) {
        try {
            String correlationId = UUID.randomUUID().toString();

            PaymentProcessingMessage message = PaymentProcessingMessage.builder()
                    .subscriptionId(subscription.getId())
                    .userId(subscription.getUser().getId())
                    .plan(subscription.getPlan())
                    .amountInCents(calculateAmount(subscription))
                    .attemptNumber(0) // Primeira tentativa de reativação
                    .eventType("REACTIVATION")
                    .correlationId(correlationId)
                    .createdAt(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    rabbitMQConfig.getPaymentExchange(),
                    rabbitMQConfig.getPaymentProcessingRoutingKey(),
                    message
            );

            log.info("Mensagem de reativação enviada para fila. SubscriptionId: {}, CorrelationId: {}",
                    subscription.getId(), correlationId);

        } catch (Exception e) {
            log.error("Erro ao enviar mensagem de reativação para fila. SubscriptionId: {}",
                    subscription.getId(), e);
            throw new RuntimeException("Falha ao enviar mensagem de pagamento", e);
        }
    }


    public void sendPaymentResult(PaymentResultMessage result) {
        try {
            rabbitTemplate.convertAndSend(
                    rabbitMQConfig.getPaymentExchange(),
                    rabbitMQConfig.getPaymentResultRoutingKey(),
                    result
            );

            log.info("Resultado de pagamento enviado. SubscriptionId: {}, Success: {}, CorrelationId: {}",
                    result.getSubscriptionId(), result.isSuccess(), result.getCorrelationId());

        } catch (Exception e) {
            log.error("Erro ao enviar resultado de pagamento. SubscriptionId: {}",
                    result.getSubscriptionId(), e);
        }
    }


    private Long calculateAmount(Subscription subscription) {
        return switch (subscription.getPlan()) {
            case BASICO -> 1990L;    // R$ 19,90
            case PREMIUM -> 3990L;   // R$ 39,90
            case FAMILIA -> 5990L;   // R$ 59,90
        };
    }
}

