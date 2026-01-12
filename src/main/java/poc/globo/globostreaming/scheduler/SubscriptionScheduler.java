package poc.globo.globostreaming.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import poc.globo.globostreaming.service.SubscriptionRenewalService;
import poc.globo.globostreaming.service.SubscriptionService;


@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final SubscriptionService subscriptionService;
    private final SubscriptionRenewalService renewalService;

    /**
     * Processa renovações automáticas de assinaturas
     * Executado todos os dias à meia-noite (00:00)
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void processAutomaticRenewals() {
        log.info("=== Iniciando processamento de renovações automáticas ===");
        try {
            renewalService.processRenewals();
            log.info("=== Processamento de renovações concluído com sucesso ===");
        } catch (Exception e) {
            log.error("Erro ao processar renovações automáticas", e);
        }
    }

    /**
     * Processa assinaturas expiradas (que não têm auto-renovação)
     * Executado todos os dias à 01:00
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void processExpiredSubscriptions() {
        log.info("=== Iniciando processamento de assinaturas expiradas ===");
        try {
            subscriptionService.processExpiredSubscriptions();
            log.info("=== Processamento de assinaturas expiradas concluído com sucesso ===");
        } catch (Exception e) {
            log.error("Erro ao processar assinaturas expiradas", e);
        }
    }
}
