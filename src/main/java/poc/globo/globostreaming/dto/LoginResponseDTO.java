package poc.globo.globostreaming.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta de autenticação")
public record LoginResponseDTO(
    @Schema(description = "Token JWT", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String token,

    @Schema(description = "Tipo do token", example = "Bearer")
    String type,

    @Schema(description = "Nome do usuário", example = "João Silva")
    String name,

    @Schema(description = "Email do usuário", example = "joao@email.com")
    String email
) {
    public LoginResponseDTO(String token, String name, String email) {
        this(token, "Bearer", name, email);
    }
}

