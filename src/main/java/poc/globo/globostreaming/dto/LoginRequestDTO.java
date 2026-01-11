package poc.globo.globostreaming.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados para login")
public record LoginRequestDTO(
        @Schema(description = "Email do usuário", example = "joao@email.com")
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        String email,

        @Schema(description = "Senha do usuário", example = "senha123")
        @NotBlank(message = "Senha é obrigatória")
        String password
) {}

