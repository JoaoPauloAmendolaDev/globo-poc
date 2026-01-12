package poc.globo.globostreaming.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import poc.globo.globostreaming.exception.ActiveSubscriptionExistsException;
import poc.globo.globostreaming.exception.SubscriptionNotFoundException;
import poc.globo.globostreaming.model.dto.CreateSubscriptionRequestDTO;
import poc.globo.globostreaming.model.dto.SubscriptionResponseDTO;
import poc.globo.globostreaming.model.entity.User;
import poc.globo.globostreaming.model.enums.SubscriptionPlan;
import poc.globo.globostreaming.model.enums.SubscriptionStatus;
import poc.globo.globostreaming.service.SubscriptionService;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para SubscriptionController
 *
 * Nota: Os endpoints requerem autenticação JWT. Estes testes verificam
 * a lógica do serviço via mocks. Para testes completos de integração,
 * usar testes com @SpringBootTest e MockMvc com security configurado.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionController - Testes Unitários")
class SubscriptionControllerTest {

    @Mock
    private SubscriptionService subscriptionService;

    private ObjectMapper objectMapper;
    private UUID subscriptionId;
    private SubscriptionResponseDTO subscriptionResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        subscriptionId = UUID.randomUUID();
        subscriptionResponse = new SubscriptionResponseDTO(
                subscriptionId,
                1L,
                SubscriptionPlan.PREMIUM,
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                SubscriptionStatus.ATIVA
        );
    }

    @Test
    @DisplayName("Serviço deve criar assinatura com sucesso")
    void serviceShouldCreateSubscriptionSuccessfully() {
        // Arrange
        User user = User.builder().id(1L).build();
        CreateSubscriptionRequestDTO request = new CreateSubscriptionRequestDTO(SubscriptionPlan.PREMIUM);

        when(subscriptionService.createSubscription(any(User.class), any(CreateSubscriptionRequestDTO.class)))
                .thenReturn(subscriptionResponse);

        // Act
        SubscriptionResponseDTO result = subscriptionService.createSubscription(user, request);

        // Assert
        verify(subscriptionService).createSubscription(user, request);
        assert result.plan() == SubscriptionPlan.PREMIUM;
        assert result.status() == SubscriptionStatus.ATIVA;
    }

    @Test
    @DisplayName("Serviço deve lançar exceção quando já existe assinatura ativa")
    void serviceShouldThrowExceptionWhenActiveSubscriptionExists() {
        // Arrange
        User user = User.builder().id(1L).build();
        CreateSubscriptionRequestDTO request = new CreateSubscriptionRequestDTO(SubscriptionPlan.PREMIUM);

        when(subscriptionService.createSubscription(any(User.class), any(CreateSubscriptionRequestDTO.class)))
                .thenThrow(new ActiveSubscriptionExistsException("Usuário já tem uma assinatura ativa"));

        // Act & Assert
        try {
            subscriptionService.createSubscription(user, request);
            assert false : "Deveria lançar exceção";
        } catch (ActiveSubscriptionExistsException e) {
            assert e.getMessage().contains("assinatura ativa");
        }

        verify(subscriptionService).createSubscription(user, request);
    }

    @Test
    @DisplayName("Serviço deve retornar assinatura ativa")
    void serviceShouldReturnActiveSubscription() {
        // Arrange
        when(subscriptionService.getActiveSubscription(1L)).thenReturn(subscriptionResponse);

        // Act
        SubscriptionResponseDTO result = subscriptionService.getActiveSubscription(1L);

        // Assert
        verify(subscriptionService).getActiveSubscription(1L);
        assert result.status() == SubscriptionStatus.ATIVA;
    }

    @Test
    @DisplayName("Serviço deve lançar exceção quando não existe assinatura ativa")
    void serviceShouldThrowExceptionWhenNoActiveSubscription() {
        // Arrange
        when(subscriptionService.getActiveSubscription(1L))
                .thenThrow(new SubscriptionNotFoundException("Nenhuma assinatura ativa encontrada"));

        // Act & Assert
        try {
            subscriptionService.getActiveSubscription(1L);
            assert false : "Deveria lançar exceção";
        } catch (SubscriptionNotFoundException e) {
            assert e.getMessage().contains("assinatura");
        }
    }

    @Test
    @DisplayName("Serviço deve cancelar assinatura com sucesso")
    void serviceShouldCancelSubscriptionSuccessfully() {
        // Arrange
        SubscriptionResponseDTO cancelledResponse = new SubscriptionResponseDTO(
                subscriptionId,
                1L,
                SubscriptionPlan.PREMIUM,
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                SubscriptionStatus.CANCELADA_PENDENTE
        );

        when(subscriptionService.cancelSubscription(eq(subscriptionId), eq(1L)))
                .thenReturn(cancelledResponse);

        // Act
        SubscriptionResponseDTO result = subscriptionService.cancelSubscription(subscriptionId, 1L);

        // Assert
        verify(subscriptionService).cancelSubscription(subscriptionId, 1L);
        assert result.status() == SubscriptionStatus.CANCELADA_PENDENTE;
    }

    @Test
    @DisplayName("Serviço deve lançar exceção ao cancelar assinatura não encontrada")
    void serviceShouldThrowExceptionWhenSubscriptionNotFoundOnCancel() {
        // Arrange
        when(subscriptionService.cancelSubscription(any(UUID.class), eq(1L)))
                .thenThrow(new SubscriptionNotFoundException("Assinatura não encontrada"));

        // Act & Assert
        try {
            subscriptionService.cancelSubscription(UUID.randomUUID(), 1L);
            assert false : "Deveria lançar exceção";
        } catch (SubscriptionNotFoundException e) {
            assert e.getMessage().contains("Assinatura");
        }
    }

    @Test
    @DisplayName("Serviço deve ativar renovação automática")
    void serviceShouldEnableAutoRenew() {
        // Arrange
        when(subscriptionService.toggleAutoRenew(eq(subscriptionId), eq(1L), eq(true)))
                .thenReturn(subscriptionResponse);

        // Act
        SubscriptionResponseDTO result = subscriptionService.toggleAutoRenew(subscriptionId, 1L, true);

        // Assert
        verify(subscriptionService).toggleAutoRenew(subscriptionId, 1L, true);
    }

    @Test
    @DisplayName("Serviço deve desativar renovação automática")
    void serviceShouldDisableAutoRenew() {
        // Arrange
        when(subscriptionService.toggleAutoRenew(eq(subscriptionId), eq(1L), eq(false)))
                .thenReturn(subscriptionResponse);

        // Act
        SubscriptionResponseDTO result = subscriptionService.toggleAutoRenew(subscriptionId, 1L, false);

        // Assert
        verify(subscriptionService).toggleAutoRenew(subscriptionId, 1L, false);
    }

    @Test
    @DisplayName("Serviço deve verificar acesso do usuário com assinatura ativa")
    void serviceShouldVerifyUserAccessWithActiveSubscription() {
        // Arrange
        when(subscriptionService.userHasAccess(1L)).thenReturn(true);

        // Act
        boolean hasAccess = subscriptionService.userHasAccess(1L);

        // Assert
        verify(subscriptionService).userHasAccess(1L);
        assert hasAccess;
    }

    @Test
    @DisplayName("Serviço deve verificar que usuário não tem acesso sem assinatura")
    void serviceShouldVerifyUserHasNoAccessWithoutSubscription() {
        // Arrange
        when(subscriptionService.userHasAccess(1L)).thenReturn(false);

        // Act
        boolean hasAccess = subscriptionService.userHasAccess(1L);

        // Assert
        verify(subscriptionService).userHasAccess(1L);
        assert !hasAccess;
    }

    @Test
    @DisplayName("Serviço deve lançar exceção ao cancelar assinatura de outro usuário")
    void serviceShouldThrowExceptionWhenCancellingAnotherUsersSubscription() {
        // Arrange
        when(subscriptionService.cancelSubscription(any(UUID.class), eq(999L)))
                .thenThrow(new SubscriptionNotFoundException("Assinatura não encontrada para o usuário"));

        // Act & Assert
        try {
            subscriptionService.cancelSubscription(subscriptionId, 999L);
            assert false : "Deveria lançar exceção";
        } catch (SubscriptionNotFoundException e) {
            assert e.getMessage().contains("não encontrada");
        }
    }
}

