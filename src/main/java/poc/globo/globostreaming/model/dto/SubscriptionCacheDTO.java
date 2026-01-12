package poc.globo.globostreaming.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import poc.globo.globostreaming.model.enums.SubscriptionPlan;
import poc.globo.globostreaming.model.enums.SubscriptionStatus;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubscriptionCacheDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private UUID subscriptionId;
    private Long userId;
    private SubscriptionPlan plan;
    private SubscriptionStatus status;
    private LocalDate expirationDate;
    private LocalDate startDate;


    public boolean isActive() {
        LocalDate today = LocalDate.now();
        boolean notExpired = expirationDate.isAfter(today) || expirationDate.isEqual(today);

        return (status == SubscriptionStatus.ATIVA ||
                status == SubscriptionStatus.CANCELADA_PENDENTE) &&
               notExpired;
    }
}

