package org.atypical.carabassa.security.controller;
 
import jakarta.validation.Valid;
import org.atypical.carabassa.security.dto.LoginRequest;
import org.atypical.carabassa.security.dto.LoginResponse;
import org.atypical.carabassa.security.dto.UserRequest;
import org.atypical.carabassa.security.dto.UserResponse;
import org.atypical.carabassa.security.entity.UserEntity;
import org.atypical.carabassa.security.repository.UserRepository;
import org.atypical.carabassa.security.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
 
import java.util.List;
import java.util.stream.Collectors;
 
@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(name = "carabassa.auth.enabled", havingValue = "true")
public class AuthController {
 
    private static final String ROLE_ADMIN = "ADMIN";
 
    @Autowired
    private AuthenticationManager authenticationManager;
 
    @Autowired
    private JwtService jwtService;
 
    @Autowired
    private UserRepository userRepository;
 
    @Autowired
    private PasswordEncoder passwordEncoder;
 
    // -------------------------------------------------------------------------
    // Login (public)
    // -------------------------------------------------------------------------
 
    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        UserEntity user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
 
        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is disabled. Please contact an administrator.");
        }
 
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
 
            // Reset failed login attempts on success
            if (user.getFailedLoginAttempts() > 0) {
                user.setFailedLoginAttempts(0);
                userRepository.save(user);
            }
 
        } catch (AuthenticationException e) {
            // Increment failed login attempts
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= 3) {
                user.setEnabled(false);
            }
            userRepository.save(user);
 
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
 
        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        return new LoginResponse(token, user.getUsername(), user.getRole(), user.getDefaultDataset());
    }
 
    // -------------------------------------------------------------------------
    // User management (ADMIN only — enforced by SecurityConfiguration)
    // -------------------------------------------------------------------------
 
    @GetMapping("/users")
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
 
    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@RequestBody @Valid UserRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Username already exists: " + request.getUsername());
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        UserEntity user = new UserEntity(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                request.getRole().toUpperCase());
        user.setEnabled(request.isEnabled());
        user.setDefaultDataset(normalizeDefaultDataset(request.getDefaultDataset()));
        return toResponse(userRepository.save(user));
    }
 
    @PutMapping("/users/{id}")
    public UserResponse update(@PathVariable Long id, @RequestBody @Valid UserRequest request) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User id=" + id + " not found"));
 
        // Prevent removing last ADMIN
        if (ROLE_ADMIN.equals(user.getRole())
                && !ROLE_ADMIN.equals(request.getRole().toUpperCase())
                && userRepository.countByRole(ROLE_ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot remove the last ADMIN user");
        }
 
        user.setUsername(request.getUsername());
        user.setRole(request.getRole().toUpperCase());
        user.setEnabled(request.isEnabled());
        if (request.getDefaultDataset() != null) {
            user.setDefaultDataset(normalizeDefaultDataset(request.getDefaultDataset()));
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        return toResponse(userRepository.save(user));
    }
 
    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User id=" + id + " not found"));
 
        if ("admin".equals(user.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete the bootstrap admin user");
        }
        if (ROLE_ADMIN.equals(user.getRole()) && userRepository.countByRole(ROLE_ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete the last ADMIN user");
        }
        userRepository.delete(user);
    }
 
    // -------------------------------------------------------------------------
    // Self-service (authenticated users)
    // -------------------------------------------------------------------------
 
    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeMyPassword(@RequestBody @Valid PasswordChangeRequest request) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
 
        if (request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }
 
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);
    }

    @PutMapping("/me/default-dataset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateMyDefaultDataset(@RequestBody DefaultDatasetRequest request) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setDefaultDataset(normalizeDefaultDataset(request.defaultDataset()));
        userRepository.save(user);
    }
 
    // -------------------------------------------------------------------------
 
    private UserResponse toResponse(UserEntity user) {
        return new UserResponse(user.getId(), user.getUsername(),
                user.getRole(), user.getDefaultDataset(), user.isEnabled(), user.getCreatedAt());
    }

    private String normalizeDefaultDataset(String defaultDataset) {
        if (defaultDataset == null || defaultDataset.isBlank()) {
            return null;
        }
        return defaultDataset;
    }
 
}
 
record PasswordChangeRequest(String password) {}
record DefaultDatasetRequest(String defaultDataset) {}
