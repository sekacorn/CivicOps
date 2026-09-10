package org.civicops.cases.task;

import jakarta.persistence.*;
import java.time.*;
import org.civicops.cases.casefile.*;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "case_task")
public class CaseTask extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "case_id", nullable = false)
  private CaseRecord caseRecord;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assigned_user_id")
  private User assignedUser;

  private LocalDate dueDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CaseTaskStatus status = CaseTaskStatus.OPEN;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CasePriority priority;

  private Instant completedAt;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by_user_id", nullable = false, updatable = false)
  private User createdBy;

  protected CaseTask() {}

  public CaseTask(
      Organization o,
      CaseRecord c,
      User creator,
      String title,
      String description,
      User assigned,
      LocalDate due,
      CasePriority priority) {
    organization = o;
    caseRecord = c;
    createdBy = creator;
    this.title = title;
    this.description = description;
    assignedUser = assigned;
    dueDate = due;
    this.priority = priority;
  }

  public Organization getOrganization() {
    return organization;
  }

  public CaseRecord getCaseRecord() {
    return caseRecord;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public User getAssignedUser() {
    return assignedUser;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public CaseTaskStatus getStatus() {
    return status;
  }

  public CasePriority getPriority() {
    return priority;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public User getCreatedBy() {
    return createdBy;
  }

  public boolean isOverdue(LocalDate today) {
    return dueDate != null
        && dueDate.isBefore(today)
        && status != CaseTaskStatus.COMPLETED
        && status != CaseTaskStatus.CANCELLED;
  }

  public void update(
      String title,
      String description,
      User assignee,
      LocalDate due,
      CasePriority priority,
      CaseTaskStatus requested) {
    if (status == CaseTaskStatus.COMPLETED || status == CaseTaskStatus.CANCELLED)
      throw new BusinessRuleException(
          "TERMINAL_TASK_NOT_EDITABLE", "Completed or cancelled tasks cannot be edited");
    if (title != null) this.title = title;
    if (description != null) this.description = description;
    if (assignee != null) assignedUser = assignee;
    if (due != null) dueDate = due;
    if (priority != null) this.priority = priority;
    if (requested == CaseTaskStatus.IN_PROGRESS) status = requested;
  }

  public void complete(Instant now) {
    if (status == CaseTaskStatus.COMPLETED || status == CaseTaskStatus.CANCELLED)
      throw new BusinessRuleException("INVALID_TASK_TRANSITION", "Task cannot be completed");
    status = CaseTaskStatus.COMPLETED;
    completedAt = now;
  }

  public void cancel() {
    if (status == CaseTaskStatus.COMPLETED || status == CaseTaskStatus.CANCELLED)
      throw new BusinessRuleException("INVALID_TASK_TRANSITION", "Task cannot be cancelled");
    status = CaseTaskStatus.CANCELLED;
  }
}
