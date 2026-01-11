package poc.globo.globostreaming.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import poc.globo.globostreaming.config.security.JwtConfig;
import poc.globo.globostreaming.dto.LoginRequestDTO;
import poc.globo.globostreaming.dto.LoginResponseDTO;
import poc.globo.globostreaming.dto.RegisterRequestDTO;
import poc.globo.globostreaming.entity.User;
import poc.globo.globostreaming.exception.CpfAlreadyExistsException;
import poc.globo.globostreaming.exception.EmailAlreadyExistsException;
import poc.globo.globostreaming.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequestDTO validRegisterRequest;
    private LoginRequestDTO validLoginRequest;
    private User user;

    @BeforeEach
    void setUp() {
        validRegisterRequest = new RegisterRequestDTO(
                "João Silva",
                "joao@email.com",
                "senha123",
                "12345678901"
        );

        validLoginRequest = new LoginRequestDTO(
                "joao@email.com",
                "senha123"
        );

        user = User.builder()
                .id(1L)
                .name("João Silva")
                .email("joao@email.com")
                .passwordHash("$2a$10$hashedPassword")
                .cpf("12345678901")
                .status("active")
                .build();
    }

    @Test
    @DisplayName("Deve registrar um novo usuário com sucesso")
    void shouldRegisterNewUserSuccessfully() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByCpf(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtConfig.generateToken(any(User.class))).thenReturn("mock-jwt-token");

        // Act
        LoginResponseDTO response = authService.register(validRegisterRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("mock-jwt-token");
        assertThat(response.type()).isEqualTo("Bearer");
        assertThat(response.name()).isEqualTo("João Silva");
        assertThat(response.email()).isEqualTo("joao@email.com");

        verify(userRepository).existsByEmail("joao@email.com");
        verify(userRepository).existsByCpf("12345678901");
        verify(passwordEncoder).encode("senha123");
        verify(userRepository).save(any(User.class));
        verify(jwtConfig).generateToken(any(User.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando email já existe")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(validRegisterRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("Email já cadastrado");

        verify(userRepository).existsByEmail("joao@email.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando CPF já existe")
    void shouldThrowExceptionWhenCpfAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByCpf(anyString())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(validRegisterRequest))
                .isInstanceOf(CpfAlreadyExistsException.class)
                .hasMessage("CPF já cadastrado");

        verify(userRepository).existsByEmail("joao@email.com");
        verify(userRepository).existsByCpf("12345678901");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando há violação de constraint de email")
    void shouldThrowExceptionWhenEmailConstraintViolation() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByCpf(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedPassword");
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry for email"));

        // Act & Assert
        assertThatThrownBy(() -> authService.register(validRegisterRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("Email já cadastrado");
    }

    @Test
    @DisplayName("Deve lançar exceção quando há violação de constraint de CPF")
    void shouldThrowExceptionWhenCpfConstraintViolation() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByCpf(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedPassword");
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry for cpf"));

        // Act & Assert
        assertThatThrownBy(() -> authService.register(validRegisterRequest))
                .isInstanceOf(CpfAlreadyExistsException.class)
                .hasMessage("CPF já cadastrado");
    }

    @Test
    @DisplayName("Deve fazer login com sucesso")
    void shouldLoginSuccessfully() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(jwtConfig.generateToken(any(User.class))).thenReturn("mock-jwt-token");

        // Act
        LoginResponseDTO response = authService.login(validLoginRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("mock-jwt-token");
        assertThat(response.type()).isEqualTo("Bearer");
        assertThat(response.name()).isEqualTo("João Silva");
        assertThat(response.email()).isEqualTo("joao@email.com");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmail("joao@email.com");
        verify(jwtConfig).generateToken(user);
    }

    @Test
    @DisplayName("Deve lançar exceção quando credenciais são inválidas")
    void shouldThrowExceptionWhenCredentialsAreInvalid() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(validLoginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Email ou senha inválidos");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário não é encontrado após autenticação")
    void shouldThrowExceptionWhenUserNotFoundAfterAuthentication() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.login(validLoginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Email ou senha inválidos");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmail("joao@email.com");
    }
}

