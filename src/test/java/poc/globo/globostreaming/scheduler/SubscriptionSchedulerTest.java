package poc.globo.globostreaming.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import poc.globo.globostreaming.service.SubscriptionRenewalService;
import poc.globo.globostreaming.service.SubscriptionService;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionScheduler - Testes Unitários")
class SubscriptionSchedulerTest {

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private SubscriptionRenewalService renewalService;

    @InjectMocks
    private SubscriptionScheduler scheduler;

    @Test
    @DisplayName("Deve processar renovações automáticas com sucesso")
    void shouldProcessAutomaticRenewalsSuccessfully() {
        // Act
        scheduler.processAutomaticRenewals();

        // Assert
        verify(renewalService, times(1)).processRenewals();
    }

    @Test
    @DisplayName("Deve processar assinaturas expiradas com sucesso")
    void shouldProcessExpiredSubscriptionsSuccessfully() {
        // Act
        scheduler.processExpiredSubscriptions();

        // Assert
        verify(subscriptionService, times(1)).processExpiredSubscriptions();
    }

    @Test
    @DisplayName("Não deve lançar exceção quando renovação falhar")
    void shouldNotThrowExceptionWhenRenewalFails() {
        // Arrange
        doThrow(new RuntimeException("Erro ao processar renovações"))
                .when(renewalService).processRenewals();

        // Act
        scheduler.processAutomaticRenewals();

        // Assert
        verify(renewalService).processRenewals();
        // Não deve lançar exceção
    }

    @Test
    @DisplayName("Não deve lançar exceção quando processamento de expiradas falhar")
    void shouldNotThrowExceptionWhenExpiredProcessingFails() {
        // Arrange
        doThrow(new RuntimeException("Erro ao processar expiradas"))
                .when(subscriptionService).processExpiredSubscriptions();

        // Act
        scheduler.processExpiredSubscriptions();

        // Assert
        verify(subscriptionService).processExpiredSubscriptions();
        // Não deve lançar exceção
    }

    @Test
    @DisplayName("Deve chamar ambos os métodos independentemente")
    void shouldCallBothMethodsIndependently() {
        // Act
        scheduler.processAutomaticRenewals();
        scheduler.processExpiredSubscriptions();

        // Assert
        verify(renewalService).processRenewals();
        verify(subscriptionService).processExpiredSubscriptions();
    }

    @Test
    @DisplayName("Deve processar renovações mesmo se processamento de expiradas falhou antes")
    void shouldProcessRenewalsEvenIfExpiredProcessingFailedBefore() {
        // Arrange
        doThrow(new RuntimeException("Erro")).when(subscriptionService).processExpiredSubscriptions();

        // Act
        scheduler.processExpiredSubscriptions();
        scheduler.processAutomaticRenewals();

        // Assert
        verify(renewalService).processRenewals();
    }
}

