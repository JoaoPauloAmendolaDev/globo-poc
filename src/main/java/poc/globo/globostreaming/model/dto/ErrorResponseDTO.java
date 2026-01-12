package poc.globo.globostreaming.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Resposta de erro padrão")
public record ErrorResponseDTO(
    @Schema(description = "Código de status HTTP")
    int status,

    @Schema(description = "Mensagem de erro")
    String message,

    @Schema(description = "Timestamp do erro", example = "2026-01-10T12:30:00")
    LocalDateTime timestamp
) {
    public ErrorResponseDTO(int status, String message) {
        this(status, message, LocalDateTime.now());
    }
}

