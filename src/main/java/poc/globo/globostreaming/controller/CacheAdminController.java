package poc.globo.globostreaming.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import poc.globo.globostreaming.model.dto.SubscriptionCacheDTO;
import poc.globo.globostreaming.service.SubscriptionCacheService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@Tag(name = "Cache Management", description = "Endpoints administrativos para gerenciar cache de assinaturas")
@RestController
@RequestMapping("/api/admin/cache")
@RequiredArgsConstructor
public class CacheAdminController {

    private final SubscriptionCacheService cacheService;

    @Operation(summary = "Verificar cache de assinatura de um usuário")
    @GetMapping("/subscription/{userId}")
    public ResponseEntity<Map<String, Object>> checkUserCache(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();

        boolean hasCache = cacheService.hasActiveSubscriptionInCache(userId);
        response.put("userId", userId);
        response.put("hasCache", hasCache);

        if (hasCache) {
            Optional<SubscriptionCacheDTO> cached = cacheService.getActiveSubscription(userId);
            cached.ifPresent(dto -> response.put("cachedData", dto));
        }

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Invalidar cache de assinatura de um usuário")
    @DeleteMapping("/subscription/{userId}")
    public ResponseEntity<Map<String, String>> invalidateUserCache(@PathVariable Long userId) {
        cacheService.invalidateCache(userId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Cache invalidado com sucesso");
        response.put("userId", String.valueOf(userId));

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Limpar todo o cache de assinaturas")
    @DeleteMapping("/subscription/all")
    public ResponseEntity<Map<String, String>> clearAllCache() {
        cacheService.clearAllCache();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Todo o cache de assinaturas foi limpo");

        return ResponseEntity.ok(response);
    }
}

