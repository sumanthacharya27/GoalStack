package com.goalstack.service;

import com.goalstack.dto.AuthResponse;
import com.goalstack.dto.LoginRequest;
import com.goalstack.dto.RegisterRequest;
import com.goalstack.dto.UserResponse;
import com.goalstack.entity.User;
import com.goalstack.exception.EmailAlreadyUsedException;
import com.goalstack.exception.InvalidCredentialsException;
import com.goalstack.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Registration, login and "who am I" logic. */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // Two people registered the same email at the same moment; the database unique key caught it.
            throw new EmailAlreadyUsedException();
        }
        return buildResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        // The same message for "no such email" and "wrong password", so nobody can probe which emails exist.
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return buildResponse(user);
    }

    public UserResponse getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(InvalidCredentialsException::new);
    }

    private AuthResponse buildResponse(User user) {
        return new AuthResponse(jwtService.createToken(user), "Bearer", jwtService.getExpirySeconds(),
                UserResponse.from(user));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
