package poc.globo.globostreaming.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import poc.globo.globostreaming.model.dto.SubscriptionResponseDTO;
import poc.globo.globostreaming.model.dto.CreateSubscriptionRequestDTO;
import poc.globo.globostreaming.model.entity.User;
import poc.globo.globostreaming.exception.GlobalExceptionHandler;
import poc.globo.globostreaming.service.SubscriptionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Assinaturas", description = "Endpoints para gerenciamento de assinaturas")
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    @Operation(summary = "Cria uma nova assinatura", description = "Cria uma nova assinatura para o usuário autenticado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Subscription created successfully",
                    content = @Content(schema = @Schema(implementation = SubscriptionResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Usuário já tem uma assinatura ativa",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class)))
    })
    public ResponseEntity<SubscriptionResponseDTO> createSubscription(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateSubscriptionRequestDTO request) {
        SubscriptionResponseDTO response = subscriptionService.createSubscription(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/active")
    @Operation(summary = "Lista assinatura ativa", description = "Lista a assinatura ativa do usuário autenticado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assinatura ativa encontrada",
                    content = @Content(schema = @Schema(implementation = SubscriptionResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Sem assinatura ativa encontrada",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class)))
    })
    public ResponseEntity<SubscriptionResponseDTO> getActiveSubscription(@AuthenticationPrincipal User user) {
        SubscriptionResponseDTO response = subscriptionService.getActiveSubscription(user.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Lista todas as assinaturas", description = "Lista todas as assinaturas do usuário autenticado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de assinaturas retornada com sucesso",
                    content = @Content(schema = @Schema(implementation = SubscriptionResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class)))
    })
    public ResponseEntity<List<SubscriptionResponseDTO>> listSubscriptions(@AuthenticationPrincipal User user) {
        List<SubscriptionResponseDTO> subscriptions = subscriptionService.listUserSubscriptions(user.getId());
        return ResponseEntity.ok(subscriptions);
    }

    @PatchMapping("/cancel")
    @Operation(summary = "Cancela uma assinatura", description = "Cancela a assinatura especificada pelo ID para o usuário autenticado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assinatura cancelada com sucesso",
                    content = @Content(schema = @Schema(implementation = SubscriptionResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Assinatura não encontrada",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Assinatura não pode ser cancelada",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class)))
    })
    public ResponseEntity<SubscriptionResponseDTO> cancelSubscription(
            @RequestParam UUID id,
            @AuthenticationPrincipal User user) {
        SubscriptionResponseDTO response = subscriptionService.cancelSubscription(id, user.getId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/auto-renew")
    @Operation(summary = "Ativa/Desativa renovação automática", description = "Ativa ou desativa a renovação automática da assinatura")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuração atualizada com sucesso",
                    content = @Content(schema = @Schema(implementation = SubscriptionResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Assinatura não encontrada",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class)))
    })
    public ResponseEntity<SubscriptionResponseDTO> toggleAutoRenew(
            @RequestParam UUID id,
            @RequestParam Boolean enabled,
            @AuthenticationPrincipal User user) {
        SubscriptionResponseDTO response = subscriptionService.toggleAutoRenew(id, user.getId(), enabled);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/can-access")
    @Operation(summary = "Verifica se usuário pode acessar o serviço",
               description = "Retorna true se o usuário tem uma assinatura válida (ATIVA ou CANCELADA_PENDENTE)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status de acesso retornado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorResponse.class)))
    })
    public ResponseEntity<Map<String, Object>> canAccess(@AuthenticationPrincipal User user) {
        boolean hasAccess = subscriptionService.userHasAccess(user.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("hasAccess", hasAccess);
        response.put("userId", user.getId());

        if (hasAccess) {
            try {
                SubscriptionResponseDTO subscription = subscriptionService.getActiveSubscription(user.getId());
                response.put("subscription", subscription);

                if (subscription.status() == poc.globo.globostreaming.model.enums.SubscriptionStatus.CANCELADA_PENDENTE) {
                    response.put("message", "Assinatura cancelada - Acesso válido até " + subscription.expirationDate());
                } else {
                    response.put("message", "Assinatura ativa");
                }
            } catch (Exception e) {
                response.put("message", "Sem assinatura ativa");
            }
        } else {
            response.put("message", "Sem acesso ao serviço");
        }

        return ResponseEntity.ok(response);
    }
}
