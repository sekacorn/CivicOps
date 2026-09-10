package org.civicops.cases.note;

import jakarta.persistence.*;
import org.civicops.cases.casefile.CaseRecord;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.shared.domain.BaseEntity;

@Entity
@Table(name = "case_note")
public class CaseNote extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "case_id", nullable = false)
  private CaseRecord caseRecord;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "author_user_id", nullable = false, updatable = false)
  private User author;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private CaseNoteType noteType;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(nullable = false)
  private boolean privateNote;

  protected CaseNote() {}

  public CaseNote(Organization o, CaseRecord c, User a, CaseNoteType t, String content, boolean p) {
    organization = o;
    caseRecord = c;
    author = a;
    noteType = t;
    this.content = content;
    privateNote = p;
  }

  public Organization getOrganization() {
    return organization;
  }

  public CaseRecord getCaseRecord() {
    return caseRecord;
  }

  public User getAuthor() {
    return author;
  }

  public CaseNoteType getNoteType() {
    return noteType;
  }

  public String getContent() {
    return content;
  }

  public boolean isPrivateNote() {
    return privateNote;
  }
}
