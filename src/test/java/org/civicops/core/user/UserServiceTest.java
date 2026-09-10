package org.civicops.core.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.civicops.core.user.dto.CreateUserRequest;
import org.civicops.shared.exception.ConflictException;
import org.civicops.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
  @Mock UserRepository users;
  @Mock PasswordEncoder encoder;

  @Test
  void createsUserWithNormalizedEmailAndEncodedPassword() {
    when(users.existsByEmail("jane@example.org")).thenReturn(false);
    when(encoder.encode("a-secure-password")).thenReturn("encoded-value");
    when(users.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var result =
        new UserService(users, encoder)
            .create(
                new CreateUserRequest(
                    " Jane ", " Smith ", " JANE@EXAMPLE.ORG ", "a-secure-password"));

    assertThat(result.email()).isEqualTo("jane@example.org");
    assertThat(result.firstName()).isEqualTo("Jane");
    verify(encoder).encode("a-secure-password");
  }

  @Test
  void rejectsDuplicateEmailAfterNormalization() {
    when(users.existsByEmail("jane@example.org")).thenReturn(true);

    assertThatThrownBy(
            () ->
                new UserService(users, encoder)
                    .create(
                        new CreateUserRequest(
                            "Jane", "Smith", " JANE@EXAMPLE.ORG ", "a-secure-password")))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void looksUpUserByNormalizedEmail() {
    User user = new User("Jane", "Smith", "jane@example.org", "hash");
    when(users.findByEmail("jane@example.org")).thenReturn(Optional.of(user));

    assertThat(new UserService(users, encoder).getByEmail(" JANE@EXAMPLE.ORG ").firstName())
        .isEqualTo("Jane");
  }

  @Test
  void rejectsUnknownUserId() {
    UUID id = UUID.randomUUID();
    when(users.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> new UserService(users, encoder).get(id))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
