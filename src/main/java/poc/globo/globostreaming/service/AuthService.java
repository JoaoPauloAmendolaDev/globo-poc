package poc.globo.globostreaming.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import poc.globo.globostreaming.config.security.JwtConfig;
import poc.globo.globostreaming.dto.LoginResponseDTO;
import poc.globo.globostreaming.dto.LoginRequestDTO;
import poc.globo.globostreaming.dto.RegisterRequestDTO;
import poc.globo.globostreaming.entity.User;
import poc.globo.globostreaming.exception.EmailAlreadyExistsException;
import poc.globo.globostreaming.exception.CpfAlreadyExistsException;
import poc.globo.globostreaming.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String CREDENCIAIS_INVALIDAS = "Email ou senha inválidos";
    public static final String CPF_CADASTRADO = "CPF já cadastrado";
    public static final String EMAIL_CADASTRADO = "Email já cadastrado";
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public LoginResponseDTO register(RegisterRequestDTO request) {
        validateUniqueUser(request);

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .cpf(request.cpf())
                .build();

        try {
            User savedUser = userRepository.save(user);
            String token = jwtConfig.generateToken(savedUser);
            return new LoginResponseDTO(token, savedUser.getName(), savedUser.getEmail());
        } catch (DataIntegrityViolationException e) {
            String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (message.contains("email")) {
                throw new EmailAlreadyExistsException(EMAIL_CADASTRADO);
            } else if (message.contains("cpf")) {
                throw new CpfAlreadyExistsException(CPF_CADASTRADO);
            }
            throw e;
        }
    }

    private void validateUniqueUser(RegisterRequestDTO request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(EMAIL_CADASTRADO);
        }

        if (userRepository.existsByCpf(request.cpf())) {
            throw new CpfAlreadyExistsException(CPF_CADASTRADO);
        }
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException(CREDENCIAIS_INVALIDAS);
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException(CREDENCIAIS_INVALIDAS));

        String token = jwtConfig.generateToken(user);

        return new LoginResponseDTO(token, user.getName(), user.getEmail());
    }
}

