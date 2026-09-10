package org.civicops.board.member;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Locale;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "board_member")
public class BoardMember extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(nullable = false)
  private String firstName;

  @Column(nullable = false)
  private String lastName;

  private String email;
  private String normalizedEmail;
  private String phone;
  private String title;

  @Column(nullable = false)
  private boolean active = true;

  @Column(nullable = false)
  private LocalDate joinedDate;

  @Column(columnDefinition = "TEXT")
  private String notes;

  protected BoardMember() {}

  public BoardMember(
      Organization o,
      User u,
      String first,
      String last,
      String email,
      String phone,
      String title,
      LocalDate joined,
      String notes) {
    organization = o;
    user = u;
    this.joinedDate = joined;
    update(first, last, email, phone, title, joined, notes);
  }

  public void update(
      String first,
      String last,
      String email,
      String phone,
      String title,
      LocalDate joined,
      String notes) {
    if (first != null) firstName = required(first, "First name");
    if (last != null) lastName = required(last, "Last name");
    if (email != null) {
      this.email = clean(email);
      normalizedEmail = normalizeEmail(email);
    }
    if (phone != null) this.phone = clean(phone);
    if (title != null) this.title = clean(title);
    if (joined != null) joinedDate = joined;
    if (notes != null) this.notes = clean(notes);
    if (firstName == null || lastName == null || joinedDate == null)
      throw new BusinessRuleException(
          "INVALID_BOARD_MEMBER", "First name, last name, and joined date are required");
  }

  public void deactivate() {
    active = false;
  }

  public void activate() {
    active = true;
  }

  public static String normalizeEmail(String v) {
    String c = clean(v);
    return c == null ? null : c.toLowerCase(Locale.ROOT);
  }

  private static String required(String v, String n) {
    if (v == null || v.isBlank())
      throw new BusinessRuleException("INVALID_BOARD_MEMBER", n + " is required");
    return v.trim();
  }

  private static String clean(String v) {
    return v == null || v.isBlank() ? null : v.trim();
  }

  public Organization getOrganization() {
    return organization;
  }

  public User getUser() {
    return user;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getEmail() {
    return email;
  }

  public String getNormalizedEmail() {
    return normalizedEmail;
  }

  public String getPhone() {
    return phone;
  }

  public String getTitle() {
    return title;
  }

  public boolean isActive() {
    return active;
  }

  public LocalDate getJoinedDate() {
    return joinedDate;
  }

  public String getNotes() {
    return notes;
  }
}
