package org.civicops.grants.expense;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface GrantExpenseRepository
    extends JpaRepository<GrantExpense, UUID>, JpaSpecificationExecutor<GrantExpense> {
  Optional<GrantExpense> findByIdAndOrganizationId(UUID id, UUID organizationId);

  @Query("select coalesce(sum(e.amount), 0) from GrantExpense e where e.grant.id = :grantId")
  BigDecimal totalForGrant(@Param("grantId") UUID grantId);

  @Query(
      "select e.category, sum(e.amount) from GrantExpense e where e.grant.id = :grantId "
          + "and e.organization.id = :organizationId group by e.category order by e.category")
  List<Object[]> totalsByCategory(
      @Param("organizationId") UUID organizationId, @Param("grantId") UUID grantId);
}
