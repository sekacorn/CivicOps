package org.civicops.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.civicops.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class ReleaseContractTest {
  @Test
  void pageResponseHasStableFrameworkIndependentMetadata() {
    var source = new PageImpl<>(List.of("one", "two"), PageRequest.of(1, 2), 5);

    var response = PageResponse.from(source);

    assertThat(response.content()).containsExactly("one", "two");
    assertThat(response.page()).isEqualTo(1);
    assertThat(response.size()).isEqualTo(2);
    assertThat(response.totalElements()).isEqualTo(5);
    assertThat(response.totalPages()).isEqualTo(3);
    assertThat(response.first()).isFalse();
    assertThat(response.last()).isFalse();
  }

  @Test
  void businessErrorsExposeStableSafeContractAndRequestPath() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/api/v1/organizations/example/grants");

    var response =
        new GlobalExceptionHandler()
            .business(new BusinessRuleException("GRANT_RULE", "Grant rule failed"), request);

    assertThat(response.getStatusCode().value()).isEqualTo(422);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo("GRANT_RULE");
    assertThat(response.getBody().path()).isEqualTo("/api/v1/organizations/example/grants");
    assertThat(response.getBody().message()).doesNotContain("Exception", "Hibernate", "SQL");
  }

  @Test
  void unexpectedErrorsDoNotExposeImplementationDetails() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/api/v1/private");

    var response =
        new GlobalExceptionHandler()
            .unexpected(new IllegalStateException("select * from secret_table"), request);

    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
    assertThat(response.getBody().message())
        .doesNotContain("secret_table", "IllegalStateException");
  }
}
