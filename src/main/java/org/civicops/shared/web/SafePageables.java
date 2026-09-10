package org.civicops.shared.web;

import java.util.Set;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class SafePageables {
  private SafePageables() {}

  public static Pageable allow(Pageable pageable, Set<String> allowed) {
    for (Sort.Order order : pageable.getSort())
      if (!allowed.contains(order.getProperty()))
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Unsupported sort field: " + order.getProperty());
    return PageRequest.of(
        pageable.getPageNumber(), Math.min(pageable.getPageSize(), 100), pageable.getSort());
  }
}
