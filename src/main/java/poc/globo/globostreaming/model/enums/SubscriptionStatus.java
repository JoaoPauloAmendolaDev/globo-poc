package poc.globo.globostreaming.model.enums;

import lombok.Getter;


@Getter
public enum SubscriptionStatus {
    ATIVA("Ativa"),
    CANCELADA_PENDENTE("Cancelada - Ativa até " ), // + expirationDate
    EXPIRADA("Expirada"),
    CANCELADA("Cancelada"),
    SUSPENSA("Suspensa");

    private final String description;

    SubscriptionStatus(String description) {
        this.description = description;
    }
}
