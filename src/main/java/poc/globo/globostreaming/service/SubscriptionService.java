package poc.globo.globostreaming.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import poc.globo.globostreaming.model.dto.SubscriptionResponseDTO;
import poc.globo.globostreaming.model.dto.CreateSubscriptionRequestDTO;
import poc.globo.globostreaming.model.dto.SubscriptionCacheDTO;
import poc.globo.globostreaming.model.entity.Subscription;
import poc.globo.globostreaming.model.entity.User;
import poc.globo.globostreaming.exception.ActiveSubscriptionExistsException;
import poc.globo.globostreaming.exception.SubscriptionNotFoundException;
import poc.globo.globostreaming.repository.SubscriptionRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static poc.globo.globostreaming.model.enums.SubscriptionStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionCacheService cacheService;


    @Transactional
    public SubscriptionResponseDTO createSubscription(User user, CreateSubscriptionRequestDTO request) {
        Optional<SubscriptionCacheDTO> cachedSubscription = cacheService.getActiveSubscription(user.getId());
        validateActiveSubscription(user, cachedSubscription);

        LocalDate startDate = LocalDate.now();
        LocalDate expirationDate = startDate.plusMonths(1);

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(request.plan())
                .startDate(startDate)
                .expirationDate(expirationDate)
                .status(ATIVA)
                .build();

        Subscription saved = subscriptionRepository.save(subscription);

        cacheService.cacheActiveSubscription(saved);
        log.info("Assinatura criada e armazenada em cache para userId: {}", user.getId());

        return toResponseDTO(saved);
    }

    private void validateActiveSubscription(User user, Optional<SubscriptionCacheDTO> cachedSubscription) {
        if (cachedSubscription.isPresent()) {
            log.debug("Assinatura ativa encontrada em cache para userId: {}", user.getId());
            throw new ActiveSubscriptionExistsException("Usuário já tem uma assinatura ativa");
        }

        if (subscriptionRepository.existsByUserIdAndStatus(user.getId(), ATIVA) ||
            subscriptionRepository.existsByUserIdAndStatus(user.getId(), CANCELADA_PENDENTE)) {
            throw new ActiveSubscriptionExistsException("Usuário já tem uma assinatura ativa");
        }
    }



    @Transactional(readOnly = true)
    public SubscriptionResponseDTO getActiveSubscription(Long userId) {
        Optional<SubscriptionCacheDTO> cachedSubscription = cacheService.getActiveSubscription(userId);

        if (cachedSubscription.isPresent()) {
            SubscriptionCacheDTO cached = cachedSubscription.get();
            log.debug("Retornando assinatura ativa do cache para userId: {}", userId);

            return new SubscriptionResponseDTO(
                    cached.getSubscriptionId(),
                    cached.getUserId(),
                    cached.getPlan(),
                    cached.getStartDate(),
                    cached.getExpirationDate(),
                    cached.getStatus()
            );
        }

        log.debug("Cache miss - Buscando assinatura ativa do banco para userId: {}", userId);

        // Buscar por ATIVA ou CANCELADA_PENDENTE
        Subscription subscription = subscriptionRepository
                .findByUserIdAndStatus(userId, ATIVA)
                .or(() -> subscriptionRepository.findByUserIdAndStatus(userId, CANCELADA_PENDENTE))
                .orElseThrow(() -> new SubscriptionNotFoundException("Nenhuma assinatura ativa encontrada"));

        cacheService.cacheActiveSubscription(subscription);
        log.debug("Assinatura ativa armazenada em cache após busca no banco para userId: {}", userId);

        return toResponseDTO(subscription);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponseDTO> listUserSubscriptions(Long userId) {
        return subscriptionRepository.findAllByUserId(userId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /**
     * Cancela uma assinatura
     * Se cancelada antes do vencimento, o usu��rio pode continuar usando até o fim do ciclo
     */
    @Transactional
    public SubscriptionResponseDTO cancelSubscription(UUID subscriptionId, Long userId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found"));

        if (!subscription.getUser().getId().equals(userId)) {
            throw new SubscriptionNotFoundException("Assinatura não encontrada para o usuário");
        }

        if (subscription.getStatus() != ATIVA) {
            throw new ActiveSubscriptionExistsException("Apenas assinaturas ativas podem ser canceladas");
        }

        LocalDate today = LocalDate.now();

        if (subscription.getExpirationDate().isAfter(today)) {
            subscription.setStatus(CANCELADA_PENDENTE);
            subscription.setCanceledAt(today);
            subscription.setAutoRenew(false);

            log.info("Assinatura marcada como CANCELADA_PENDENTE. Usuário pode usar até {}. SubscriptionId: {}",
                    subscription.getExpirationDate(), subscriptionId);
        } else {
            subscription.setStatus(CANCELADA);
            subscription.setCanceledAt(today);

            log.info("Assinatura cancelada imediatamente (já expirada). SubscriptionId: {}", subscriptionId);
        }

        Subscription saved = subscriptionRepository.save(subscription);

        if (subscription.getStatus() == CANCELADA) {
            cacheService.invalidateCache(userId);
            log.info("Cache invalidado para userId: {}", userId);
        } else {
            cacheService.cacheActiveSubscription(saved);
            log.info("Cache atualizado com status CANCELADA_PENDENTE para userId: {}", userId);
        }

        return toResponseDTO(saved);
    }

    @Transactional
    public SubscriptionResponseDTO toggleAutoRenew(UUID subscriptionId, Long userId, Boolean enabled) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found"));

        if (!subscription.getUser().getId().equals(userId)) {
            throw new SubscriptionNotFoundException("Assinatura não encontrada para o usuário");
        }

        subscription.setAutoRenew(enabled);
        Subscription saved = subscriptionRepository.save(subscription);

        log.info("Auto-renovação {} para subscriptionId: {}", enabled ? "ATIVADA" : "DESATIVADA", subscriptionId);

        return toResponseDTO(saved);
    }


    @Transactional(readOnly = true)
    public boolean userHasAccess(Long userId) {
        try {
            Optional<SubscriptionCacheDTO> cachedSubscription = cacheService.getActiveSubscription(userId);
            if (cachedSubscription.isPresent()) {
                return cachedSubscription.get().isActive();
            }

            Optional<Subscription> activeSubscription = subscriptionRepository
                    .findByUserIdAndStatus(userId, ATIVA);

            if (activeSubscription.isPresent()) {
                return activeSubscription.get().getExpirationDate().isAfter(LocalDate.now()) ||
                       activeSubscription.get().getExpirationDate().isEqual(LocalDate.now());
            }

            Optional<Subscription> pendingCancellation = subscriptionRepository
                    .findByUserIdAndStatus(userId, CANCELADA_PENDENTE);

            return pendingCancellation.filter(subscription -> subscription.getExpirationDate().isAfter(LocalDate.now()) ||
                    subscription.getExpirationDate().isEqual(LocalDate.now())).isPresent();

        } catch (Exception e) {
            log.error("Erro ao verificar acesso do usuário userId: {}", userId, e);
            return false;
        }
    }

    @Transactional
    public void processExpiredSubscriptions() {
        log.info("Processando assinaturas expiradas...");

        LocalDate today = LocalDate.now();

        List<Subscription> expiredActiveSubscriptions = subscriptionRepository
                .findByStatusAndExpirationDateBefore(ATIVA, today);

        for (Subscription subscription : expiredActiveSubscriptions) {
            subscription.setStatus(EXPIRADA);
            subscriptionRepository.save(subscription);

            cacheService.invalidateCache(subscription.getUser().getId());
            log.info("Assinatura ATIVA expirada e cache invalidado para userId: {}",
                    subscription.getUser().getId());
        }

        // Processar assinaturas CANCELADA_PENDENTE que expiraram
        List<Subscription> expiredPendingCancellations = subscriptionRepository
                .findByStatusAndExpirationDateBefore(CANCELADA_PENDENTE, today);

        for (Subscription subscription : expiredPendingCancellations) {
            subscription.setStatus(CANCELADA);
            subscriptionRepository.save(subscription);

            cacheService.invalidateCache(subscription.getUser().getId());
            log.info("Assinatura CANCELADA_PENDENTE finalizada para CANCELADA. UserId: {}",
                    subscription.getUser().getId());
        }

        log.info("Total de assinaturas processadas - ATIVA->EXPIRADA: {}, CANCELADA_PENDENTE->CANCELADA: {}",
                expiredActiveSubscriptions.size(),
                expiredPendingCancellations.size());
    }

    private SubscriptionResponseDTO toResponseDTO(Subscription subscription) {
        return new SubscriptionResponseDTO(
                subscription.getId(),
                subscription.getUser().getId(),
                subscription.getPlan(),
                subscription.getStartDate(),
                subscription.getExpirationDate(),
                subscription.getStatus()
        );
    }
}
