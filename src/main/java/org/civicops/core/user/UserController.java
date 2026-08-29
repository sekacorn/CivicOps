package org.civicops.core.user;

import jakarta.validation.Valid;
import org.civicops.core.security.CurrentUserProvider;
import org.springframework.security.access.AccessDeniedException;
import org.civicops.core.user.dto.CreateUserRequest;
import org.civicops.core.user.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService users;
    private final CurrentUserProvider currentUser;

    public UserController(UserService users, CurrentUserProvider currentUser) {
        this.users = users;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return users.create(request);
    }

    @GetMapping("/{userId}")
    public UserResponse get(@PathVariable UUID userId) {
        if (!currentUser.currentUserId().equals(userId)) {
            throw new AccessDeniedException("Users may only read their own profile");
        }
        return users.get(userId);
    }
}
