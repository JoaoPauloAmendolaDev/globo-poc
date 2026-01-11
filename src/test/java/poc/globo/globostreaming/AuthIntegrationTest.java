package poc.globo.globostreaming;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import poc.globo.globostreaming.dto.LoginRequestDTO;
import poc.globo.globostreaming.dto.LoginResponseDTO;
import poc.globo.globostreaming.dto.RegisterRequestDTO;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Integration Tests - Auth Flow")
class AuthIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Deve completar fluxo completo: Register -> Login -> Acessar endpoint protegido")
    void shouldCompleteFullAuthFlow() throws Exception {
        // 1. Registrar novo usuário
        RegisterRequestDTO registerRequest = new RegisterRequestDTO(
                "João Silva",
                "joao.integration@email.com",
                "senha123",
                "12345678901"
        );

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.name").value("João Silva"))
                .andExpect(jsonPath("$.email").value("joao.integration@email.com"))
                .andReturn();

        String registerResponseJson = registerResult.getResponse().getContentAsString();
        LoginResponseDTO registerResponse = objectMapper.readValue(registerResponseJson, LoginResponseDTO.class);
        String registerToken = registerResponse.token();

        // 2. Fazer login com o mesmo usuário
        LoginRequestDTO loginRequest = new LoginRequestDTO(
                "joao.integration@email.com",
                "senha123"
        );

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.name").value("João Silva"))
                .andExpect(jsonPath("$.email").value("joao.integration@email.com"))
                .andReturn();

        String loginResponseJson = loginResult.getResponse().getContentAsString();
        LoginResponseDTO loginResponse = objectMapper.readValue(loginResponseJson, LoginResponseDTO.class);
        String loginToken = loginResponse.token();

        // 3. Acessar endpoint protegido com token do registro
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + registerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("João Silva"))
                .andExpect(jsonPath("$.email").value("joao.integration@email.com"))
                .andExpect(jsonPath("$.status").value("active"));

        // 4. Acessar endpoint protegido com token do login
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + loginToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("João Silva"))
                .andExpect(jsonPath("$.email").value("joao.integration@email.com"))
                .andExpect(jsonPath("$.status").value("active"));
    }

    @Test
    @DisplayName("Deve retornar 401 ao acessar endpoint protegido sem token")
    void shouldReturn401WhenAccessingProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve retornar 401 ao acessar endpoint protegido com token inválido")
    void shouldReturn401WhenAccessingProtectedEndpointWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve retornar 409 ao tentar registrar email duplicado")
    void shouldReturn409WhenRegisteringDuplicateEmail() throws Exception {
        // Registrar primeiro usuário
        RegisterRequestDTO firstRequest = new RegisterRequestDTO(
                "João Silva",
                "duplicate@email.com",
                "senha123",
                "11111111111"
        );

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        // Tentar registrar segundo usuário com mesmo email
        RegisterRequestDTO duplicateRequest = new RegisterRequestDTO(
                "Maria Silva",
                "duplicate@email.com", // mesmo email
                "senha456",
                "22222222222"
        );

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email já cadastrado"));
    }

    @Test
    @DisplayName("Deve retornar 409 ao tentar registrar CPF duplicado")
    void shouldReturn409WhenRegisteringDuplicateCpf() throws Exception {
        // Registrar primeiro usuário
        RegisterRequestDTO firstRequest = new RegisterRequestDTO(
                "João Silva",
                "joao@email.com",
                "senha123",
                "33333333333"
        );

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        // Tentar registrar segundo usuário com mesmo CPF
        RegisterRequestDTO duplicateRequest = new RegisterRequestDTO(
                "Maria Silva",
                "maria@email.com",
                "senha456",
                "33333333333" // mesmo CPF
        );

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("CPF já cadastrado"));
    }
}

