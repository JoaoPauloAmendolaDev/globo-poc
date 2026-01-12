package poc.globo.globostreaming.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import poc.globo.globostreaming.model.entity.Subscription;
import poc.globo.globostreaming.model.enums.SubscriptionStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);

    List<Subscription> findAllByUserId(Long userId);

    boolean existsByUserIdAndStatus(Long userId, SubscriptionStatus status);

    List<Subscription> findByStatusAndExpirationDateBefore(SubscriptionStatus status, LocalDate date);

    List<Subscription> findByStatusAndExpirationDateAndAutoRenew(
            SubscriptionStatus status,
            LocalDate expirationDate,
            Boolean autoRenew
    );
}
