package poc.globo.globostreaming.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import poc.globo.globostreaming.service.SubscriptionRenewalService;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Renewal Management", description = "Endpoints administrativos para gerenciar renovações de assinaturas")
@RestController
@RequestMapping("/api/admin/renewals")
@RequiredArgsConstructor
public class RenewalAdminController {

    private final SubscriptionRenewalService renewalService;

    @Operation(summary = "Processar renovações manualmente",
               description = "Executa o processo de renovação automática imediatamente (útil para testes)")
    @PostMapping("/process")
    public ResponseEntity<Map<String, String>> processRenewals() {
        renewalService.processRenewals();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Processamento de renovações iniciado com sucesso");
        response.put("status", "COMPLETED");

        return ResponseEntity.ok(response);
    }
}

