package org.civicops.board.agenda;

import jakarta.persistence.*;
import org.civicops.board.meeting.*;
import org.civicops.core.organization.Organization;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "board_agenda_item")
public class BoardAgendaItem extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "meeting_id")
  private BoardMeeting meeting;

  @Column(nullable = false)
  private int sequenceNumber;

  @Column(nullable = false)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AgendaItemType itemType;

  private String presenter;
  private Integer estimatedMinutes;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AgendaItemStatus status = AgendaItemStatus.PENDING;

  protected BoardAgendaItem() {}

  public BoardAgendaItem(
      BoardMeeting m,
      int sequence,
      String title,
      String description,
      AgendaItemType type,
      String presenter,
      Integer minutes) {
    organization = m.getOrganization();
    meeting = m;
    sequenceNumber = sequence;
    this.title = title;
    this.description = clean(description);
    itemType = type;
    this.presenter = clean(presenter);
    estimatedMinutes = minutes;
    validate();
  }

  public void update(
      Integer sequence,
      String title,
      String description,
      AgendaItemType type,
      String presenter,
      Integer minutes) {
    if (status == AgendaItemStatus.COMPLETED || status == AgendaItemStatus.SKIPPED)
      throw new BusinessRuleException(
          "FINAL_AGENDA_ITEM_IMMUTABLE", "Final agenda items cannot be edited");
    if (sequence != null) sequenceNumber = sequence;
    if (title != null) this.title = title.trim();
    if (description != null) this.description = clean(description);
    if (type != null) itemType = type;
    if (presenter != null) this.presenter = clean(presenter);
    if (minutes != null) estimatedMinutes = minutes;
    validate();
  }

  private void validate() {
    if (sequenceNumber <= 0 || title == null || title.isBlank() || itemType == null)
      throw new BusinessRuleException(
          "INVALID_AGENDA_ITEM", "Positive sequence, title, and type are required");
    if (estimatedMinutes != null && estimatedMinutes <= 0)
      throw new BusinessRuleException(
          "INVALID_AGENDA_DURATION", "Estimated minutes must be positive");
  }

  public void transition(AgendaItemStatus target) {
    boolean ok =
        switch (status) {
          case PENDING ->
              target == AgendaItemStatus.IN_PROGRESS || target == AgendaItemStatus.SKIPPED;
          case IN_PROGRESS ->
              target == AgendaItemStatus.COMPLETED || target == AgendaItemStatus.SKIPPED;
          case COMPLETED, SKIPPED -> false;
        };
    if (!ok)
      throw new BusinessRuleException(
          "INVALID_AGENDA_TRANSITION",
          "Agenda item cannot transition from " + status + " to " + target);
    status = target;
  }

  private static String clean(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }

  public Organization getOrganization() {
    return organization;
  }

  public BoardMeeting getMeeting() {
    return meeting;
  }

  public int getSequenceNumber() {
    return sequenceNumber;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public AgendaItemType getItemType() {
    return itemType;
  }

  public String getPresenter() {
    return presenter;
  }

  public Integer getEstimatedMinutes() {
    return estimatedMinutes;
  }

  public AgendaItemStatus getStatus() {
    return status;
  }
}
