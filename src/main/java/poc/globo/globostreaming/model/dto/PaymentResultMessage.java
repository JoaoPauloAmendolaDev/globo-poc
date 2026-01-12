package poc.globo.globostreaming.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para resultado do processamento de pagamento
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResultMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID subscriptionId;
    private Long userId;
    private boolean success;
    private String message;
    private Integer attemptNumber;
    private LocalDateTime processedAt;
    private String correlationId;
    private String errorCode;
}

