package poc.globo.globostreaming.config.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import poc.globo.globostreaming.model.entity.User;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtConfig Tests")
class JwtConfigTest {

    private JwtConfig jwtConfig;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();

        // Configurar secret key e expiration via reflection
        String secretKey = Base64.getEncoder().encodeToString("test-secret-key-for-jwt-authentication-must-be-at-least-256-bits".getBytes());
        ReflectionTestUtils.setField(jwtConfig, "secretKey", secretKey);
        ReflectionTestUtils.setField(jwtConfig, "jwtExpiration", 86400000L); // 24 horas

        testUser = User.builder()
                .id(1L)
                .name("João Silva")
                .email("joao@email.com")
                .passwordHash("$2a$10$hashedPassword")
                .cpf("12345678901")
                .status("active")
                .build();
    }

    @Test
    @DisplayName("Deve gerar token JWT válido")
    void shouldGenerateValidJwtToken() {
        // Act
        String token = jwtConfig.generateToken(testUser);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT tem 3 partes: header.payload.signature
    }

    @Test
    @DisplayName("Deve extrair username (email) do token")
    void shouldExtractUsernameFromToken() {
        // Arrange
        String token = jwtConfig.generateToken(testUser);

        // Act
        String username = jwtConfig.extractUsername(token);

        // Assert
        assertThat(username).isEqualTo("joao@email.com");
    }

    @Test
    @DisplayName("Deve extrair nome do token")
    void shouldExtractNameFromToken() {
        // Arrange
        String token = jwtConfig.generateToken(testUser);

        // Act
        String name = jwtConfig.extractClaim(token, claims -> claims.get("name", String.class));

        // Assert
        assertThat(name).isEqualTo("João Silva");
    }

    @Test
    @DisplayName("Deve extrair email do token")
    void shouldExtractEmailFromToken() {
        // Arrange
        String token = jwtConfig.generateToken(testUser);

        // Act
        String email = jwtConfig.extractClaim(token, claims -> claims.get("email", String.class));

        // Assert
        assertThat(email).isEqualTo("joao@email.com");
    }

    @Test
    @DisplayName("Deve extrair CPF do token")
    void shouldExtractCpfFromToken() {
        // Arrange
        String token = jwtConfig.generateToken(testUser);

        // Act
        String cpf = jwtConfig.extractClaim(token, claims -> claims.get("cpf", String.class));

        // Assert
        assertThat(cpf).isEqualTo("12345678901");
    }

    @Test
    @DisplayName("Deve validar token com UserDetails correto")
    void shouldValidateTokenWithCorrectUserDetails() {
        // Arrange
        String token = jwtConfig.generateToken(testUser);

        // Act
        boolean isValid = jwtConfig.isTokenValid(token, testUser);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Deve invalidar token com UserDetails incorreto")
    void shouldInvalidateTokenWithIncorrectUserDetails() {
        // Arrange
        String token = jwtConfig.generateToken(testUser);

        User differentUser = User.builder()
                .id(2L)
                .name("Maria Silva")
                .email("maria@email.com")
                .passwordHash("$2a$10$hashedPassword")
                .cpf("98765432100")
                .status("active")
                .build();

        // Act
        boolean isValid = jwtConfig.isTokenValid(token, differentUser);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Deve gerar tokens diferentes para usuários diferentes")
    void shouldGenerateDifferentTokensForDifferentUsers() {
        // Arrange
        User user1 = User.builder()
                .id(1L)
                .name("João Silva")
                .email("joao@email.com")
                .passwordHash("$2a$10$hashedPassword")
                .cpf("12345678901")
                .status("active")
                .build();

        User user2 = User.builder()
                .id(2L)
                .name("Maria Silva")
                .email("maria@email.com")
                .passwordHash("$2a$10$hashedPassword")
                .cpf("98765432100")
                .status("active")
                .build();

        // Act
        String token1 = jwtConfig.generateToken(user1);
        String token2 = jwtConfig.generateToken(user2);

        // Assert
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("Token deve conter todos os claims necessários")
    void tokenShouldContainAllNecessaryClaims() {
        // Arrange & Act
        String token = jwtConfig.generateToken(testUser);

        // Assert
        String username = jwtConfig.extractUsername(token);
        String name = jwtConfig.extractClaim(token, claims -> claims.get("name", String.class));
        String email = jwtConfig.extractClaim(token, claims -> claims.get("email", String.class));
        String cpf = jwtConfig.extractClaim(token, claims -> claims.get("cpf", String.class));

        assertThat(username).isEqualTo("joao@email.com");
        assertThat(name).isEqualTo("João Silva");
        assertThat(email).isEqualTo("joao@email.com");
        assertThat(cpf).isEqualTo("12345678901");
    }
}

