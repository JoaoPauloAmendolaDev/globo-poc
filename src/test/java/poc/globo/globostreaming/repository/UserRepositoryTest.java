package poc.globo.globostreaming.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import poc.globo.globostreaming.entity.User;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("UserRepository Tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Deve encontrar usuário por email")
    void shouldFindUserByEmail() {
        // Arrange
        User user = User.builder()
                .name("João Silva")
                .email("joao@email.com")
                .passwordHash("$2a$10$hashedPassword")
                .cpf("12345678901")
                .status("active")
                .build();
        userRepository.save(user);

        // Act
        Optional<User> found = userRepository.findByEmail("joao@email.com");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("João Silva");
        assertThat(found.get().getEmail()).isEqualTo("joao@email.com");
    }

    @Test
    @DisplayName("Deve retornar vazio quando email não existe")
    void shouldReturnEmptyWhenEmailNotExists() {
        // Act
        Optional<User> found = userRepository.findByEmail("naoexiste@email.com");

        // Assert
        assertThat(found).isEmpty();
    }


    @Test
    @DisplayName("Deve retornar true quando email existe")
    void shouldReturnTrueWhenEmailExists() {
        // Arrange
        User user = User.builder()
                .name("João Silva")
                .email("joao@email.com")
                .passwordHash("$2a$10$hashedPassword")
                .cpf("12345678901")
                .status("active")
                .build();
        userRepository.save(user);

        // Act
        boolean exists = userRepository.existsByEmail("joao@email.com");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Deve retornar false quando email não existe")
    void shouldReturnFalseWhenEmailNotExists() {
        // Act
        boolean exists = userRepository.existsByEmail("naoexiste@email.com");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Deve retornar true quando CPF existe")
    void shouldReturnTrueWhenCpfExists() {
        // Arrange
        User user = User.builder()
                .name("João Silva")
                .email("joao@email.com")
                .passwordHash("$2a$10$hashedPassword")
                .cpf("12345678901")
                .status("active")
                .build();
        userRepository.save(user);

        // Act
        boolean exists = userRepository.existsByCpf("12345678901");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Deve retornar false quando CPF não existe")
    void shouldReturnFalseWhenCpfNotExists() {
        // Act
        boolean exists = userRepository.existsByCpf("00000000000");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Deve salvar usuário com sucesso")
    void shouldSaveUserSuccessfully() {
        // Arrange
        User user = User.builder()
                .name("João Silva")
                .email("joao@email.com")
                .passwordHash("$2a$10$hashedPassword")
                .cpf("12345678901")
                .status("active")
                .build();

        // Act
        User saved = userRepository.save(user);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("João Silva");
        assertThat(saved.getEmail()).isEqualTo("joao@email.com");
        assertThat(saved.getCpf()).isEqualTo("12345678901");
        assertThat(saved.getStatus()).isEqualTo("active");
        assertThat(saved.getCreatedAt()).isNotNull();
    }
}

