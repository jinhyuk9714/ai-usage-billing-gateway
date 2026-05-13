package io.github.sungjh.aiusagebillinggateway.auth;

import io.github.sungjh.aiusagebillinggateway.domain.UserAccount;
import io.github.sungjh.aiusagebillinggateway.repository.UserAccountRepository;
import io.github.sungjh.aiusagebillinggateway.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserAccountRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthTokenResponse signup(AuthRequest request) {
        String email = request.email().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        try {
            UserAccount user = userRepository.save(new UserAccount(
                    email,
                    passwordEncoder.encode(request.password())));
            return tokenFor(user);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists", exception);
        }
    }

    @Transactional(readOnly = true)
    public AuthTokenResponse login(AuthRequest request) {
        UserAccount user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return tokenFor(user);
    }

    private AuthTokenResponse tokenFor(UserAccount user) {
        return new AuthTokenResponse(jwtService.createToken(user.getId(), user.getEmail()), "Bearer");
    }
}
