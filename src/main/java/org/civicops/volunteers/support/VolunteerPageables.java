package org.civicops.volunteers.support;

import java.util.Set;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.Pageable;

public final class VolunteerPageables {
  private VolunteerPageables() {}

  public static Pageable allow(Pageable pageable, Set<String> allowed) {
    return SafePageables.allow(pageable, allowed);
  }
}
