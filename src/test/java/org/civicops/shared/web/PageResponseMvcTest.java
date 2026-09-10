package org.civicops.shared.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.civicops.core.security.JwtService;
import org.civicops.core.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(PagedTestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PageResponseMvcTest {
  @Autowired MockMvc mvc;
  @MockitoBean JwtService jwtService;
  @MockitoBean UserRepository userRepository;

  @Test
  void pageIsSerializedThroughStableExternalContract() throws Exception {
    mvc.perform(get("/api/v1/test-page"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0]").value("one"))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.totalElements").value(5))
        .andExpect(jsonPath("$.totalPages").value(3))
        .andExpect(jsonPath("$.first").value(false))
        .andExpect(jsonPath("$.last").value(false))
        .andExpect(jsonPath("$.pageable").doesNotExist())
        .andExpect(jsonPath("$.sort").doesNotExist());
  }
}

@RestController
class PagedTestController {
  @GetMapping("/api/v1/test-page")
  Page<String> page() {
    return new PageImpl<>(List.of("one", "two"), PageRequest.of(1, 2), 5);
  }
}
