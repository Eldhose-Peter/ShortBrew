package com.epproject.ShortBrew.service;

import com.epproject.ShortBrew.exception.ConflictException;
import com.epproject.ShortBrew.exception.UnauthorizedException;
import com.epproject.ShortBrew.exception.ValidationException;
import com.epproject.ShortBrew.model.User;
import com.epproject.ShortBrew.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class UserService {

    private final UserRepository userRepository;
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User registerUser(String email, String password, String fullName) {
        validateEmail(email);
        validatePassword(password);

        if (userRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("Email is already registered");
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        return userRepository.save(email, hashedPassword, fullName);
    }

    public User authenticateUser(String email, String password) {
        // Validation of request structure
        validateEmail(email);
        if (password == null || password.isEmpty()) {
            throw new ValidationException("Password cannot be empty");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!BCrypt.checkpw(password, user.hashedPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        if (!user.isActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }

        return user;
    }

    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }

    private void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("Malformed email address");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters");
        }
    }
}
