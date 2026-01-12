package poc.globo.globostreaming.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados do usuário")
public record UserResponseDTO(
    @Schema(description = "Nome do usuário", example = "João Silva")
    String name,

    @Schema(description = "Email do usuário", example = "joao@email.com")
    String email,

    @Schema(description = "Status do usuário", example = "active")
    String status
) {}

