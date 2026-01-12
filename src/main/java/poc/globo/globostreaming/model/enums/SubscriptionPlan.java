package poc.globo.globostreaming.model.enums;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public enum SubscriptionPlan {
    BASICO("BASICO", new BigDecimal("19.90")),
    PREMIUM("PREMIUM", new BigDecimal("39.90")),
    FAMILIA("FAMILIA", new BigDecimal("59.90"));

    private final String name;
    private final BigDecimal price;

    SubscriptionPlan(String name, BigDecimal price) {
        this.name = name;
        this.price = price;
    }
}

