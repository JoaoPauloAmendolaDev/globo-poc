package poc.globo.globostreaming.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import poc.globo.globostreaming.model.enums.SubscriptionPlan;
import poc.globo.globostreaming.model.enums.SubscriptionStatus;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Subscription response data")
public record SubscriptionResponseDTO(
        @Schema(description = "Subscription ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "User ID", example = "1")
        Long userId,

        @Schema(description = "Subscription plan", example = "PREMIUM")
        SubscriptionPlan plan,

        @Schema(description = "Subscription start date", example = "2025-03-10")
        LocalDate startDate,

        @Schema(description = "Subscription expiration date", example = "2025-04-10")
        LocalDate expirationDate,

        @Schema(description = "Subscription status", example = "ACTIVE")
        SubscriptionStatus status
) {}

