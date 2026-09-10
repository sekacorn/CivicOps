package org.civicops.board.member;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface BoardOfficerAssignmentRepository
    extends JpaRepository<BoardOfficerAssignment, UUID> {
  @Query(
      "select o from BoardOfficerAssignment o where o.organization.id=:org and o.boardMember.id=:member order by o.startDate desc")
  List<BoardOfficerAssignment> forMember(@Param("org") UUID org, @Param("member") UUID member);

  Optional<BoardOfficerAssignment> findByIdAndOrganizationId(UUID id, UUID org);

  @Query(
      "select distinct o.boardMember.id from BoardOfficerAssignment o where o.organization.id=:org and o.officerRole=:role and o.active=true")
  List<UUID> activeMemberIds(@Param("org") UUID org, @Param("role") BoardOfficerRole role);
}
