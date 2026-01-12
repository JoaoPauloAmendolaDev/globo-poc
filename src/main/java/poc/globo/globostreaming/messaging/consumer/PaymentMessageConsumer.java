package poc.globo.globostreaming.messaging.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import poc.globo.globostreaming.config.RabbitMQConfig;
import poc.globo.globostreaming.messaging.producer.PaymentMessageProducer;
import poc.globo.globostreaming.model.dto.PaymentProcessingMessage;
import poc.globo.globostreaming.model.dto.PaymentResultMessage;
import poc.globo.globostreaming.model.entity.Subscription;
import poc.globo.globostreaming.repository.SubscriptionRepository;
import poc.globo.globostreaming.service.PaymentService;
import poc.globo.globostreaming.service.SubscriptionCacheService;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static poc.globo.globostreaming.model.enums.SubscriptionStatus.*;


@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMessageConsumer {

    private final PaymentService paymentService;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionCacheService cacheService;
    private final PaymentMessageProducer messageProducer;

    /**
     * Listener que processa mensagens de pagamento
     * Configurado para retry automático (3 tentativas)
     */
    @RabbitListener(queues = "${rabbitmq.payment.queue.processing}")
    public void processPayment(PaymentProcessingMessage message) {
        log.info("Mensagem de pagamento recebida. SubscriptionId: {}, Tipo: {}, Tentativa: {}, CorrelationId: {}",
                message.getSubscriptionId(),
                message.getEventType(),
                message.getAttemptNumber() + 1,
                message.getCorrelationId());

        try {
            Subscription subscription = subscriptionRepository.findById(message.getSubscriptionId())
                    .orElseThrow(() -> new RuntimeException("Subscription not found: " + message.getSubscriptionId()));

            boolean paymentSuccess = paymentService.processRenewalPayment(subscription);

            if (paymentSuccess) {
                handlePaymentSuccess(subscription, message);
            } else {
                handlePaymentFailure(subscription, message);
            }

        } catch (Exception e) {
            log.error("Erro ao processar pagamento. SubscriptionId: {}, CorrelationId: {}",
                    message.getSubscriptionId(), message.getCorrelationId(), e);

            sendToDeadLetterQueue(message, e);
        }
    }

    /**
     * Listener que processa resultados de pagamentos
     */
    @RabbitListener(queues = "${rabbitmq.payment.queue.result}")
    public void processPaymentResult(PaymentResultMessage result) {
        log.info("Resultado de pagamento recebido. SubscriptionId: {}, Success: {}, CorrelationId: {}",
                result.getSubscriptionId(),
                result.isSuccess(),
                result.getCorrelationId());

        // Aqui você pode implementar lógicas adicionais:
        // - Enviar notificação por email
        // - Atualizar dashboard
        // - Registrar em auditoria
        // - Enviar webhook para sistema externo
    }


    private void handlePaymentSuccess(Subscription subscription, PaymentProcessingMessage message) {
        log.info("Pagamento processado com SUCESSO. SubscriptionId: {}, CorrelationId: {}",
                subscription.getId(), message.getCorrelationId());

        LocalDate newExpirationDate = subscription.getExpirationDate().plusMonths(1);
        subscription.setExpirationDate(newExpirationDate);
        subscription.setRenewalAttempts(0);
        subscription.setStatus(ATIVA);

        subscriptionRepository.save(subscription);

        cacheService.cacheActiveSubscription(subscription);

        PaymentResultMessage result = PaymentResultMessage.builder()
                .subscriptionId(subscription.getId())
                .userId(subscription.getUser().getId())
                .success(true)
                .message("Pagamento processado e assinatura renovada com sucesso")
                .attemptNumber(message.getAttemptNumber())
                .processedAt(LocalDateTime.now())
                .correlationId(message.getCorrelationId())
                .build();

        messageProducer.sendPaymentResult(result);

        log.info("Assinatura renovada até: {}. SubscriptionId: {}",
                newExpirationDate, subscription.getId());
    }


    private void handlePaymentFailure(Subscription subscription, PaymentProcessingMessage message) {
        int currentAttempts = subscription.getRenewalAttempts();
        subscription.setRenewalAttempts(currentAttempts + 1);

        log.warn("Falha no pagamento. SubscriptionId: {}, Tentativa: {}/3, CorrelationId: {}",
                subscription.getId(),
                subscription.getRenewalAttempts(),
                message.getCorrelationId());

        // Se atingiu 3 tentativas, suspender
        if (subscription.getRenewalAttempts() >= 3) {
            subscription.setStatus(SUSPENSA);
            subscriptionRepository.save(subscription);
            cacheService.invalidateCache(subscription.getUser().getId());

            log.error("Assinatura SUSPENSA após 3 tentativas falhadas. SubscriptionId: {}, UserId: {}",
                    subscription.getId(), subscription.getUser().getId());

            PaymentResultMessage result = PaymentResultMessage.builder()
                    .subscriptionId(subscription.getId())
                    .userId(subscription.getUser().getId())
                    .success(false)
                    .message("Assinatura suspensa após 3 tentativas de pagamento")
                    .attemptNumber(subscription.getRenewalAttempts())
                    .processedAt(LocalDateTime.now())
                    .correlationId(message.getCorrelationId())
                    .errorCode("MAX_ATTEMPTS_REACHED")
                    .build();

            messageProducer.sendPaymentResult(result);
        } else {
            subscriptionRepository.save(subscription);

            PaymentResultMessage result = PaymentResultMessage.builder()
                    .subscriptionId(subscription.getId())
                    .userId(subscription.getUser().getId())
                    .success(false)
                    .message("Falha no pagamento - Tentativa " + subscription.getRenewalAttempts() + "/3")
                    .attemptNumber(subscription.getRenewalAttempts())
                    .processedAt(LocalDateTime.now())
                    .correlationId(message.getCorrelationId())
                    .errorCode("PAYMENT_FAILED")
                    .build();

            messageProducer.sendPaymentResult(result);
        }
    }


    private void sendToDeadLetterQueue(PaymentProcessingMessage message, Exception error) {
        log.error("Enviando mensagem para DLQ. SubscriptionId: {}, Error: {}",
                message.getSubscriptionId(), error.getMessage());
    }
}

