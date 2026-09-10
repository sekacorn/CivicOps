package org.civicops.board.member;

import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface BoardTermRepository extends JpaRepository<BoardTerm, UUID> {
  List<BoardTerm> findAllByOrganizationIdAndBoardMemberIdOrderByTermStartDesc(
      UUID org, UUID member);

  Optional<BoardTerm> findByIdAndOrganizationId(UUID id, UUID org);

  @Query(
      "select count(t)>0 from BoardTerm t where t.organization.id=:org and t.boardMember.id=:member and t.status='ACTIVE' and t.boardMember.active=true and :date between t.termStart and t.termEnd")
  boolean eligible(
      @Param("org") UUID org, @Param("member") UUID member, @Param("date") LocalDate date);

  @Query(
      "select count(distinct t.boardMember.id) from BoardTerm t where t.organization.id=:org and t.status='ACTIVE' and t.boardMember.active=true and :date between t.termStart and t.termEnd")
  long eligibleCount(@Param("org") UUID org, @Param("date") LocalDate date);

  @Query(
      "select distinct t.boardMember.id from BoardTerm t where t.organization.id=:org and t.status=:status")
  List<UUID> memberIds(@Param("org") UUID org, @Param("status") BoardTermStatus status);
}
