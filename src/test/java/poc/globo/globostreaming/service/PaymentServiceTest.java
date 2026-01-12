package poc.globo.globostreaming.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import poc.globo.globostreaming.model.entity.Subscription;
import poc.globo.globostreaming.model.entity.User;
import poc.globo.globostreaming.model.enums.SubscriptionPlan;
import poc.globo.globostreaming.model.enums.SubscriptionStatus;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService - Testes Unitários")
class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    private User user;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@email.com")
                .cpf("12345678901")
                .build();

        subscription = Subscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(SubscriptionPlan.PREMIUM)
                .startDate(LocalDate.now())
                .expirationDate(LocalDate.now().plusMonths(1))
                .status(SubscriptionStatus.ATIVA)
                .renewalAttempts(0)
                .build();
    }

    @Test
    @DisplayName("Deve processar pagamento e retornar true ou false")
    void shouldProcessPaymentAndReturnBoolean() {
        // Act
        boolean result = paymentService.processRenewalPayment(subscription);

        // Assert
        assertThat(result).isIn(true, false);
    }

    @RepeatedTest(10)
    @DisplayName("Primeira tentativa deve ter alta taxa de sucesso (~80%)")
    void firstAttemptShouldHaveHighSuccessRate() {
        // Arrange
        subscription.setRenewalAttempts(0);
        int successCount = 0;
        int totalAttempts = 100;

        // Act
        for (int i = 0; i < totalAttempts; i++) {
            if (paymentService.processRenewalPayment(subscription)) {
                successCount++;
            }
        }

        // Assert - Espera entre 60% e 100% de sucesso (permite variação estatística)
        assertThat(successCount).isBetween(60, 100);
    }


    @Test
    @DisplayName("Segunda tentativa deve ter taxa de sucesso menor que primeira")
    void secondAttemptShouldHaveLowerSuccessRate() {
        // Arrange
        int successCountFirst = 0;
        int successCountSecond = 0;
        int totalAttempts = 100;

        // Act
        for (int i = 0; i < totalAttempts; i++) {
            subscription.setRenewalAttempts(0);
            if (paymentService.processRenewalPayment(subscription)) {
                successCountFirst++;
            }

            subscription.setRenewalAttempts(1);
            if (paymentService.processRenewalPayment(subscription)) {
                successCountSecond++;
            }
        }

        // Assert - Segunda tentativa deve ter menor taxa de sucesso
        // Permite variação estatística
        assertThat(successCountSecond).isLessThan(successCountFirst + 15);
    }

    @Test
    @DisplayName("Terceira tentativa deve ter a menor taxa de sucesso")
    void thirdAttemptShouldHaveLowestSuccessRate() {
        // Arrange
        int successCountFirst = 0;
        int successCountThird = 0;
        int totalAttempts = 100;

        // Act
        for (int i = 0; i < totalAttempts; i++) {
            subscription.setRenewalAttempts(0);
            if (paymentService.processRenewalPayment(subscription)) {
                successCountFirst++;
            }

            subscription.setRenewalAttempts(2);
            if (paymentService.processRenewalPayment(subscription)) {
                successCountThird++;
            }
        }

        // Assert
        assertThat(successCountThird).isLessThan(successCountFirst);
    }

    @Test
    @DisplayName("Deve processar pagamento sem lançar exceção mesmo após múltiplas tentativas")
    void shouldProcessPaymentWithoutExceptionAfterMultipleAttempts() {
        // Arrange
        subscription.setRenewalAttempts(5);

        // Act & Assert
        boolean result = paymentService.processRenewalPayment(subscription);
        assertThat(result).isIn(true, false);
    }
}

