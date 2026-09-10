package org.civicops.cases.casefile;

import jakarta.persistence.*;
import java.time.LocalDate;
import org.civicops.cases.client.Client;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "case_record")
public class CaseRecord extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "client_id", nullable = false)
  private Client client;

  @Column(nullable = false, length = 100)
  private String caseNumber;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private CaseType caseType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CasePriority priority;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CaseStatus status = CaseStatus.OPEN;

  @Column(nullable = false)
  private LocalDate openedDate;

  private LocalDate closedDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assigned_user_id")
  private User assignedUser;

  @Column(length = 200)
  private String programName;

  @Column(length = 200)
  private String intakeSource;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by_user_id", nullable = false, updatable = false)
  private User createdBy;

  protected CaseRecord() {}

  public CaseRecord(
      Organization o,
      Client c,
      User u,
      String number,
      String title,
      String description,
      CaseType type,
      CasePriority priority,
      LocalDate opened,
      String program,
      String intake) {
    organization = o;
    client = c;
    createdBy = u;
    caseNumber = number;
    this.title = title;
    this.description = description;
    caseType = type;
    this.priority = priority;
    openedDate = opened;
    programName = program;
    intakeSource = intake;
  }

  public Organization getOrganization() {
    return organization;
  }

  public Client getClient() {
    return client;
  }

  public String getCaseNumber() {
    return caseNumber;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public CaseType getCaseType() {
    return caseType;
  }

  public CasePriority getPriority() {
    return priority;
  }

  public CaseStatus getStatus() {
    return status;
  }

  public LocalDate getOpenedDate() {
    return openedDate;
  }

  public LocalDate getClosedDate() {
    return closedDate;
  }

  public User getAssignedUser() {
    return assignedUser;
  }

  public String getProgramName() {
    return programName;
  }

  public String getIntakeSource() {
    return intakeSource;
  }

  public User getCreatedBy() {
    return createdBy;
  }

  public boolean isTerminal() {
    return status == CaseStatus.CLOSED || status == CaseStatus.CANCELLED;
  }

  public void update(
      String title,
      String description,
      CaseType type,
      CasePriority priority,
      String program,
      String intake) {
    if (isTerminal())
      throw new BusinessRuleException(
          "TERMINAL_CASE_NOT_EDITABLE", "Closed or cancelled cases cannot be edited");
    if (title != null) this.title = title;
    if (description != null) this.description = description;
    if (type != null) caseType = type;
    if (priority != null) this.priority = priority;
    if (program != null) programName = program;
    if (intake != null) intakeSource = intake;
  }

  public void assign(User user) {
    if (isTerminal())
      throw new BusinessRuleException(
          "TERMINAL_CASE_NOT_EDITABLE", "Terminal cases cannot be assigned");
    assignedUser = user;
  }

  public void transition(CaseStatus target, LocalDate today) {
    boolean ok =
        switch (status) {
          case OPEN -> target == CaseStatus.IN_PROGRESS || target == CaseStatus.CANCELLED;
          case IN_PROGRESS -> target == CaseStatus.ON_HOLD || target == CaseStatus.CLOSED;
          case ON_HOLD -> target == CaseStatus.IN_PROGRESS;
          default -> false;
        };
    if (!ok)
      throw new BusinessRuleException(
          "INVALID_CASE_TRANSITION", "Case cannot transition from " + status + " to " + target);
    status = target;
    if (target == CaseStatus.CLOSED || target == CaseStatus.CANCELLED) closedDate = today;
  }
}
