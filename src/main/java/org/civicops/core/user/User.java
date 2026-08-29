package org.civicops.core.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.civicops.shared.domain.BaseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

@Entity
@Table(name = "app_user")
public class User extends BaseEntity {
    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean emailVerified;

    protected User() {
    }

    User(String firstName, String lastName, String normalizedEmail, String passwordHash) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = normalizedEmail;
        this.passwordHash = passwordHash;
    }

    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public boolean isActive() { return active; }
    public boolean isEmailVerified() { return emailVerified; }
    public boolean passwordMatches(String plaintextPassword, PasswordEncoder encoder) {
        return encoder.matches(plaintextPassword, passwordHash);
    }
}
