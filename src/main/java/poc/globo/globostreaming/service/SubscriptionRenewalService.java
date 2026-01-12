package poc.globo.globostreaming.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import poc.globo.globostreaming.messaging.producer.PaymentMessageProducer;
import poc.globo.globostreaming.model.entity.Subscription;
import poc.globo.globostreaming.model.enums.SubscriptionStatus;
import poc.globo.globostreaming.repository.SubscriptionRepository;

import java.time.LocalDate;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionRenewalService {

    private static final int MAX_RENEWAL_ATTEMPTS = 3;

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentMessageProducer messageProducer;
    private final SubscriptionCacheService cacheService;


    @Transactional
    public void processRenewals() {
        LocalDate today = LocalDate.now();

        log.info("Iniciando processamento de renovações automáticas para {}", today);

        List<Subscription> subscriptionsToRenew = subscriptionRepository
                .findByStatusAndExpirationDateAndAutoRenew(SubscriptionStatus.ATIVA, today, true);

        log.info("Encontradas {} assinaturas para renovação", subscriptionsToRenew.size());

        int sentToQueue = 0;
        int errors = 0;

        for (Subscription subscription : subscriptionsToRenew) {
            try {
                messageProducer.sendRenewalPaymentMessage(subscription);
                sentToQueue++;

                log.debug("Renovação enviada para fila. SubscriptionId: {}", subscription.getId());

            } catch (Exception e) {
                log.error("Erro ao enviar renovação para fila. SubscriptionId: {}",
                        subscription.getId(), e);
                errors++;
            }
        }

        log.info("Renovações enviadas para processamento assíncrono - Enviadas: {}, Erros: {}",
                sentToQueue, errors);
    }


    @Transactional
    public void reactivateSubscription(Subscription subscription) {
        if (subscription.getStatus() != SubscriptionStatus.SUSPENSA) {
            throw new IllegalStateException("Apenas assinaturas suspensas podem ser reativadas");
        }

        log.info("Tentando reativar subscriptionId: {}", subscription.getId());

        try {
            messageProducer.sendReactivationPaymentMessage(subscription);
            log.info("Reativação enviada para processamento assíncrono. SubscriptionId: {}",
                    subscription.getId());
        } catch (Exception e) {
            log.error("Erro ao enviar reativação para fila. SubscriptionId: {}",
                    subscription.getId(), e);
            throw new IllegalStateException("Falha ao enviar mensagem de reativação", e);
        }
    }

    public int getRemainingAttempts(Subscription subscription) {
        return Math.max(0, MAX_RENEWAL_ATTEMPTS - subscription.getRenewalAttempts());
    }
}
