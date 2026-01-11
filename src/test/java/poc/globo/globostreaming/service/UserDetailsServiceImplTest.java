package poc.globo.globostreaming.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import poc.globo.globostreaming.entity.User;
import poc.globo.globostreaming.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailsServiceImpl Tests")
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("Deve carregar usuário por email com sucesso")
    void shouldLoadUserByUsernameSuccessfully() {
        // Arrange
        User user = User.builder()
                .id(1L)
                .name("João Silva")
                .email("joao@email.com")
                .passwordHash("$2a$10$hashedPassword")
                .cpf("12345678901")
                .status("active")
                .build();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("joao@email.com");

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("joao@email.com");
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$hashedPassword");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();

        verify(userRepository).findByEmail("joao@email.com");
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário não é encontrado")
    void shouldThrowExceptionWhenUserNotFound() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("naoexiste@email.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Usuário não encontrado: naoexiste@email.com");

        verify(userRepository).findByEmail("naoexiste@email.com");
    }

    @Test
    @DisplayName("Usuário inativo deve retornar isEnabled = false")
    void inactiveUserShouldReturnIsEnabledFalse() {
        // Arrange
        User inactiveUser = User.builder()
                .id(1L)
                .name("João Silva")
                .email("joao@email.com")
                .passwordHash("$2a$10$hashedPassword")
                .cpf("12345678901")
                .status("inactive")
                .build();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(inactiveUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("joao@email.com");

        // Assert
        assertThat(userDetails.isEnabled()).isFalse();
        assertThat(userDetails.isAccountNonLocked()).isFalse();
    }
}

