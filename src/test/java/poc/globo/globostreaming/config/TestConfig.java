package poc.globo.globostreaming.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import poc.globo.globostreaming.messaging.producer.PaymentMessageProducer;
import poc.globo.globostreaming.model.dto.PaymentResultMessage;
import poc.globo.globostreaming.model.entity.Subscription;
import poc.globo.globostreaming.service.SubscriptionCacheService;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
@Profile("test")
public class TestConfig {

    @Bean
    @Primary
    public SubscriptionCacheService mockSubscriptionCacheService() {
        SubscriptionCacheService mockService = mock(SubscriptionCacheService.class);

        // Configure mocks básicos
        doNothing().when(mockService).cacheActiveSubscription(any());
        doNothing().when(mockService).invalidateCache(anyLong());
        doNothing().when(mockService).clearAllCache();
        when(mockService.hasActiveSubscriptionInCache(anyLong())).thenReturn(false);
        when(mockService.getActiveSubscription(anyLong())).thenReturn(Optional.empty());

        return mockService;
    }

    @Bean
    @Primary
    public PaymentMessageProducer mockPaymentMessageProducer() {
        PaymentMessageProducer mockProducer = mock(PaymentMessageProducer.class);

        // Configure mocks básicos
        doNothing().when(mockProducer).sendRenewalPaymentMessage(any(Subscription.class));
        doNothing().when(mockProducer).sendReactivationPaymentMessage(any(Subscription.class));
        doNothing().when(mockProducer).sendPaymentResult(any(PaymentResultMessage.class));

        return mockProducer;
    }

    @Bean
    @Primary
    public RabbitMQConfig mockRabbitMQConfig() {
        RabbitMQConfig mockConfig = mock(RabbitMQConfig.class);

        when(mockConfig.getPaymentExchange()).thenReturn("test.payment.exchange");
        when(mockConfig.getPaymentProcessingQueue()).thenReturn("test.payment.processing.queue");
        when(mockConfig.getPaymentResultQueue()).thenReturn("test.payment.result.queue");
        when(mockConfig.getPaymentProcessingRoutingKey()).thenReturn("test.payment.process");
        when(mockConfig.getPaymentResultRoutingKey()).thenReturn("test.payment.result");

        return mockConfig;
    }
}



