package poc.globo.globostreaming.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import poc.globo.globostreaming.model.entity.Subscription;
import poc.globo.globostreaming.model.enums.SubscriptionPlan;

import java.util.Random;

/**
 * Serviço simulado de processamento de pagamentos
 *
 * Em um ambiente real, este serviço se integraria com:
 * - Gateway de pagamento (Stripe, PayPal, etc.)
 * - Sistema de faturamento
 * - Processador de cartões de crédito
 */
@Slf4j
@Service
public class PaymentService {

    private final Random random = new Random();


    public boolean processRenewalPayment(Subscription subscription) {
        try {
            log.info("Processando pagamento de renovação para subscriptionId: {}, tentativa: {}",
                    subscription.getId(), subscription.getRenewalAttempts() + 1);

            Thread.sleep(100);

            int successRate = getSuccessRate(subscription.getRenewalAttempts());
            boolean success = random.nextInt(100) < successRate;

            if (success) {
                log.info("Pagamento processado com sucesso para subscriptionId: {}", subscription.getId());
            } else {
                log.warn("Falha no processamento de pagamento para subscriptionId: {}", subscription.getId());
            }

            return success;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Erro ao processar pagamento para subscriptionId: {}", subscription.getId(), e);
            return false;
        }
    }


    private int getSuccessRate(int attempts) {
        return switch (attempts) {
            case 0 -> 80; // Primeira tentativa: 80%
            case 1 -> 60; // Segunda tentativa: 60%
            case 2 -> 40; // Terceira tentativa: 40%
            default -> 0; // Sem mais tentativas
        };
    }
}

