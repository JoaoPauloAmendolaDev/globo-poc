package poc.globo.globostreaming.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import poc.globo.globostreaming.model.enums.SubscriptionPlan;

@Schema(description = "Request to create a new subscription")
public record CreateSubscriptionRequestDTO(
        @Schema(description = "Subscription plan", example = "PREMIUM", required = true)
        @NotNull(message = "Plan is required")
        SubscriptionPlan plan
) {}

