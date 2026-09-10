package org.civicops.grants.support;

import java.util.Set;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;

public final class GrantPageables {
  private GrantPageables() {}

  public static Pageable allow(Pageable pageable, Set<String> allowed) {
    return SafePageables.allow(pageable, allowed);
  }
}
