package poc.globo.globostreaming.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import poc.globo.globostreaming.model.enums.SubscriptionPlan;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para mensagens de processamento de pagamento no RabbitMQ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessingMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID subscriptionId;
    private Long userId;
    private SubscriptionPlan plan;
    private Long amountInCents;
    private Integer attemptNumber;
    private String eventType; // RENEWAL, REACTIVATION

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private String correlationId; // Para rastreamento

    public boolean isFirstAttempt() {
        return attemptNumber == null || attemptNumber == 0;
    }

    public boolean isLastAttempt() {
        return attemptNumber != null && attemptNumber >= 2; // 0, 1, 2 = 3 tentativas
    }
}

