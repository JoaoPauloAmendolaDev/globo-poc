package poc.globo.globostreaming.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import poc.globo.globostreaming.dto.ErrorResponseDTO;
import poc.globo.globostreaming.dto.UserResponseDTO;
import poc.globo.globostreaming.entity.User;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Usuários", description = "Endpoints de gerenciamento de usuários")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    @Operation(summary = "Dados do usuário logado", description = "Retorna os dados do usuário autenticado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dados retornados com sucesso",
                    content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser(@AuthenticationPrincipal User user) {
        UserResponseDTO response = new UserResponseDTO(
                user.getName(),
                user.getEmail(),
                user.getStatus()
        );
        return ResponseEntity.ok(response);
    }
}

