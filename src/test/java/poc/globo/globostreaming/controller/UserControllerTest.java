package poc.globo.globostreaming.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import poc.globo.globostreaming.dto.LoginResponseDTO;
import poc.globo.globostreaming.dto.RegisterRequestDTO;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("UserController Tests")
class UserControllerTest {

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
    @DisplayName("GET /api/users/me - Deve retornar dados do usuário autenticado")
    void shouldReturnCurrentUserData() throws Exception {
        // Arrange - criar e fazer login do usuário
        RegisterRequestDTO registerRequest = new RegisterRequestDTO(
                "João User Controller",
                "joao.usercontroller@email.com",
                "senha123",
                "33344455566"
        );

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        LoginResponseDTO loginResponse = objectMapper.readValue(responseJson, LoginResponseDTO.class);
        String token = loginResponse.token();

        // Act & Assert
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("João User Controller"))
                .andExpect(jsonPath("$.email").value("joao.usercontroller@email.com"))
                .andExpect(jsonPath("$.status").value("active"));
    }

    @Test
    @DisplayName("GET /api/users/me - Deve retornar 401 quando não autenticado")
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }
}

