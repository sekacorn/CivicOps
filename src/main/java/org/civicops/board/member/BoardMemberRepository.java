package org.civicops.board.member;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface BoardMemberRepository
    extends JpaRepository<BoardMember, UUID>, JpaSpecificationExecutor<BoardMember> {
  Optional<BoardMember> findByIdAndOrganizationId(UUID id, UUID org);

  Optional<BoardMember> findByOrganizationIdAndUserIdAndActiveTrue(UUID org, UUID user);

  boolean existsByOrganizationIdAndUserId(UUID org, UUID user);

  long countByOrganizationIdAndActiveTrue(UUID org);
}
