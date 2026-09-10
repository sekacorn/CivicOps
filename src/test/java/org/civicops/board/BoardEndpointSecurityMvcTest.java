package org.civicops.board;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.*;
import java.util.*;
import org.civicops.board.agenda.*;
import org.civicops.board.committee.*;
import org.civicops.board.meeting.*;
import org.civicops.board.member.*;
import org.civicops.board.minutes.*;
import org.civicops.board.motion.*;
import org.civicops.board.reporting.*;
import org.civicops.board.reporting.BoardReportDtos;
import org.civicops.board.resolution.*;
import org.civicops.board.security.BoardAccessService;
import org.civicops.core.security.*;
import org.civicops.core.user.UserRepository;
import org.civicops.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({
  BoardMemberController.class,
  BoardCommitteeController.class,
  BoardMeetingController.class,
  BoardAgendaController.class,
  BoardMotionController.class,
  BoardMinutesController.class,
  BoardResolutionController.class,
  BoardReportingController.class
})
@Import({
  CoreSecurityConfiguration.class,
  JwtAuthenticationFilter.class,
  SecurityErrorWriter.class,
  CurrentUserProvider.class,
  GlobalExceptionHandler.class
})
@TestPropertySource(properties = "civicops.security.jwt.secret=0123456789abcdef0123456789abcdef")
class BoardEndpointSecurityMvcTest {
  @Autowired MockMvc mvc;
  @MockitoBean BoardMemberService members;
  @MockitoBean BoardCommitteeService committees;
  @MockitoBean BoardMeetingService meetings;
  @MockitoBean BoardAgendaService agenda;
  @MockitoBean BoardMotionService motions;
  @MockitoBean BoardMinutesService minutes;
  @MockitoBean BoardResolutionService resolutions;
  @MockitoBean BoardReportingService reports;
  @MockitoBean BoardAccessService access;
  @MockitoBean JwtService jwt;
  @MockitoBean UserRepository users;

  @Test
  void unauthenticatedBoardRouteIs401() throws Exception {
    mvc.perform(get("/api/v1/organizations/{o}/board-meetings", UUID.randomUUID()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void boardManagerCanManageGovernanceWorkflow() throws Exception {
    UUID o = UUID.randomUUID(),
        meeting = UUID.randomUUID(),
        member = UUID.randomUUID(),
        motion = UUID.randomUUID();
    when(access.userId()).thenReturn(UUID.randomUUID());
    mvc.perform(
            post("/api/v1/organizations/{o}/board-members", o)
                .with(authentication(auth()))
                .contentType("application/json")
                .content(
                    "{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\",\"joinedDate\":\"2030-01-01\"}"))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/organizations/{o}/board-committees", o)
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"name\":\"Finance\"}"))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/organizations/{o}/board-meetings", o)
                .with(authentication(auth()))
                .contentType("application/json")
                .content(
                    "{\"title\":\"Annual Meeting\",\"meetingType\":\"ANNUAL\",\"startDateTime\":\"2030-01-01T12:00:00Z\",\"endDateTime\":\"2030-01-01T14:00:00Z\",\"quorumRequired\":3}"))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/organizations/{o}/board-meetings/{m}/agenda-items", o, meeting)
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"sequenceNumber\":1,\"title\":\"Budget\",\"itemType\":\"ACTION\"}"))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/organizations/{o}/board-meetings/{m}/motions", o, meeting)
                .with(authentication(auth()))
                .contentType("application/json")
                .content(
                    "{\"motionText\":\"Approve budget\",\"movedByBoardMemberId\":\""
                        + member
                        + "\"}"))
        .andExpect(status().isCreated());
    mvc.perform(
            put("/api/v1/organizations/{o}/board-meetings/{m}/minutes", o, meeting)
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"content\":\"Meeting minutes\"}"))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/v1/organizations/{o}/board-motions/{m}/resolution", o, motion)
                .with(authentication(auth()))
                .contentType("application/json")
                .content(
                    "{\"resolutionNumber\":\"2030-1\",\"title\":\"Budget\",\"text\":\"Resolved\",\"adoptedDate\":\"2030-01-01\"}"))
        .andExpect(status().isCreated());
    verify(access, atLeast(6)).requireBoardManagement(o);
    verify(access).requireMinutesManagement(o);
  }

  @Test
  void boardMemberCanReadMaterialsAndCastOnlyOwnVote() throws Exception {
    UUID o = UUID.randomUUID(), motion = UUID.randomUUID();
    when(access.userId()).thenReturn(UUID.randomUUID());
    when(meetings.list(eq(o), any(), any(), any(), any(), any())).thenReturn(Page.empty());
    mvc.perform(get("/api/v1/organizations/{o}/board-meetings", o).with(authentication(auth())))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/v1/organizations/{o}/board-motions/{m}/votes/me", o, motion)
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"choice\":\"YES\"}"))
        .andExpect(status().isCreated());
    verify(access).requireBoardRead(o);
    verify(access).requireVotingAccess(o);
  }

  @Test
  void boardMemberCannotRetrieveIndividualVoteLedger() throws Exception {
    UUID o = UUID.randomUUID();
    doThrow(new AccessDeniedException("manager only")).when(access).requireBoardManagement(o);
    mvc.perform(
            get("/api/v1/organizations/{o}/board-motions/{m}/votes", o, UUID.randomUUID())
                .with(authentication(auth())))
        .andExpect(status().isForbidden());
  }

  @Test
  void boardMemberCannotReadDraftMinutes() throws Exception {
    UUID o = UUID.randomUUID(), m = UUID.randomUUID();
    when(access.canManage(o)).thenReturn(false);
    doThrow(new AccessDeniedException("approved only")).when(minutes).get(o, m, false);
    mvc.perform(
            get("/api/v1/organizations/{o}/board-meetings/{m}/minutes", o, m)
                .with(authentication(auth())))
        .andExpect(status().isForbidden());
  }

  @Test
  void boardMemberSummaryOmitsPrivateContactFields() throws Exception {
    UUID o = UUID.randomUUID();
    when(members.list(eq(o), any(), any(), any(), any()))
        .thenReturn(
            new PageImpl<>(
                List.of(
                    new BoardMemberDtos.Summary(
                        UUID.randomUUID(),
                        null,
                        "Ada",
                        "Lovelace",
                        "Chair",
                        true,
                        LocalDate.of(2030, 1, 1),
                        Instant.EPOCH))));
    mvc.perform(get("/api/v1/organizations/{o}/board-members", o).with(authentication(auth())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].firstName").value("Ada"))
        .andExpect(jsonPath("$.content[0].email").doesNotExist())
        .andExpect(jsonPath("$.content[0].phone").doesNotExist())
        .andExpect(jsonPath("$.content[0].notes").doesNotExist());
  }

  @Test
  void programManagerCanReadPiiFreeAggregateReport() throws Exception {
    UUID o = UUID.randomUUID();
    when(reports.summary(eq(o), any(), any()))
        .thenReturn(
            new BoardReportDtos.Summary(
                o, null, null, 5, 2, 1, new java.math.BigDecimal("80.00"), 0, 1, 0, 1));
    mvc.perform(
            get("/api/v1/organizations/{o}/board-reports/summary", o).with(authentication(auth())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.activeBoardMembers").value(5))
        .andExpect(jsonPath("$.email").doesNotExist())
        .andExpect(jsonPath("$.voteChoice").doesNotExist());
    verify(access).requireGovernanceReporting(o);
  }

  @Test
  void viewerHasNoBoardReadAccess() throws Exception {
    UUID o = UUID.randomUUID();
    doThrow(new AccessDeniedException("no board access")).when(access).requireBoardRead(o);
    mvc.perform(get("/api/v1/organizations/{o}/board-meetings", o).with(authentication(auth())))
        .andExpect(status().isForbidden());
  }

  @Test
  void programManagerCannotMutateBoard() throws Exception {
    UUID o = UUID.randomUUID();
    doThrow(new AccessDeniedException("manager only")).when(access).requireBoardManagement(o);
    mvc.perform(
            post("/api/v1/organizations/{o}/board-committees", o)
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"name\":\"Audit\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void crossOrganizationRequestIs403() throws Exception {
    UUID o = UUID.randomUUID();
    doThrow(new AccessDeniedException("cross organization")).when(access).requireBoardRead(o);
    mvc.perform(get("/api/v1/organizations/{o}/board-resolutions", o).with(authentication(auth())))
        .andExpect(status().isForbidden());
  }

  @Test
  void unsupportedBoardSortReturns400() throws Exception {
    UUID o = UUID.randomUUID();
    mvc.perform(
            get("/api/v1/organizations/{o}/board-members?sort=email", o)
                .with(authentication(auth())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void invalidMeetingAndVoteRequestsReturn400() throws Exception {
    UUID o = UUID.randomUUID();
    mvc.perform(
            post("/api/v1/organizations/{o}/board-meetings", o)
                .with(authentication(auth()))
                .contentType("application/json")
                .content(
                    "{\"title\":\"Meeting\",\"meetingType\":\"REGULAR\",\"startDateTime\":\"2030-01-01T12:00:00Z\",\"endDateTime\":\"2030-01-01T14:00:00Z\",\"quorumRequired\":0}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/v1/organizations/{o}/board-motions/{m}/votes/me", o, UUID.randomUUID())
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  private UsernamePasswordAuthenticationToken auth() {
    return UsernamePasswordAuthenticationToken.authenticated(
        new CivicOpsPrincipal(UUID.randomUUID(), "board@example.org"), null, List.of());
  }
}
