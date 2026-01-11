package poc.globo.globostreaming.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import poc.globo.globostreaming.dto.LoginRequestDTO;
import poc.globo.globostreaming.dto.RegisterRequestDTO;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private WebApplicationContext context;

    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/register - Deve registrar usuário com sucesso")
    void shouldRegisterUserSuccessfully() throws Exception {
        // Arrange
        RegisterRequestDTO request = new RegisterRequestDTO(
                "João Test Controller",
                "joao.controller@email.com",
                "senha123",
                "98765432100"
        );

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.name").value("João Test Controller"))
                .andExpect(jsonPath("$.email").value("joao.controller@email.com"));
    }

    @Test
    @DisplayName("POST /api/auth/register - Deve retornar 400 quando dados são inválidos")
    void shouldReturn400WhenRegisterDataIsInvalid() throws Exception {
        // Arrange
        RegisterRequestDTO invalidRequest = new RegisterRequestDTO(
                "", // nome vazio
                "email-invalido", // email inválido
                "123", // senha muito curta
                "123" // CPF inválido
        );

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/register - Deve retornar 409 quando email já existe")
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {
        // Arrange - criar usuário primeiro
        RegisterRequestDTO firstRequest = new RegisterRequestDTO(
                "João Silva",
                "duplicate.controller@email.com",
                "senha123",
                "11122233344"
        );

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)));

        // Tentar registrar novamente com mesmo email
        RegisterRequestDTO request = new RegisterRequestDTO(
                "João Silva",
                "duplicate.controller@email.com",
                "senha123",
                "55566677788"
        );

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email já cadastrado"));
    }

    @Test
    @DisplayName("POST /api/auth/register - Deve retornar 409 quando CPF já existe")
    void shouldReturn409WhenCpfAlreadyExists() throws Exception {
        // Arrange - criar usuário primeiro
        RegisterRequestDTO firstRequest = new RegisterRequestDTO(
                "João Silva",
                "joao.cpf@email.com",
                "senha123",
                "99988877766"
        );

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)));

        // Tentar registrar com mesmo CPF
        RegisterRequestDTO request = new RegisterRequestDTO(
                "João Silva",
                "outro.email@email.com",
                "senha123",
                "99988877766"
        );

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("CPF já cadastrado"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Deve fazer login com sucesso")
    void shouldLoginSuccessfully() throws Exception {
        // Arrange - registrar usuário primeiro
        RegisterRequestDTO registerRequest = new RegisterRequestDTO(
                "João Login",
                "joao.login@email.com",
                "senha123",
                "44455566677"
        );

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        LoginRequestDTO request = new LoginRequestDTO(
                "joao.login@email.com",
                "senha123"
        );

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.name").value("João Login"))
                .andExpect(jsonPath("$.email").value("joao.login@email.com"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Deve retornar 401 quando credenciais são inválidas")
    void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
        // Arrange
        LoginRequestDTO request = new LoginRequestDTO(
                "naoexiste@email.com",
                "senhaErrada"
        );

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/login - Deve retornar 400 quando dados são inválidos")
    void shouldReturn400WhenLoginDataIsInvalid() throws Exception {
        // Arrange
        LoginRequestDTO invalidRequest = new LoginRequestDTO(
                "email-invalido", // email inválido
                "" // senha vazia
        );

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}

