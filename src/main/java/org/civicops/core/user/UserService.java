package org.civicops.core.user;

import org.civicops.core.user.dto.CreateUserRequest;
import org.civicops.core.user.dto.UserResponse;
import org.civicops.shared.exception.ConflictException;
import org.civicops.shared.exception.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException("DUPLICATE_USER_EMAIL", "A user with this email already exists");
        }
        User user = new User(request.firstName().trim(), request.lastName().trim(), email,
                passwordEncoder.encode(request.password()));
        return UserResponse.from(users.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        return UserResponse.from(requireEntity(id));
    }

    @Transactional(readOnly = true)
    public UserResponse getByEmail(String email) {
        String normalized = normalizeEmail(email);
        return users.findByEmail(normalized).map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User", normalized));
    }

    public User requireEntity(UUID id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
