package org.civicops.volunteers.volunteer;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.shared.domain.BaseEntity;

@Entity
@Table(
    name = "volunteer",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_volunteer_org_email",
          columnNames = {"organization_id", "email"}),
      @UniqueConstraint(
          name = "uk_volunteer_org_user",
          columnNames = {"organization_id", "user_id"})
    })
public class Volunteer extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(nullable = false, length = 100)
  private String firstName;

  @Column(nullable = false, length = 100)
  private String lastName;

  @Column(nullable = false, length = 320)
  private String email;

  @Column(length = 50)
  private String phone;

  @Column(length = 250)
  private String addressLine1;

  @Column(length = 250)
  private String addressLine2;

  @Column(length = 120)
  private String city;

  @Column(length = 120)
  private String state;

  @Column(length = 30)
  private String postalCode;

  @Column(length = 2)
  private String country;

  @Column(length = 200)
  private String emergencyContactName;

  @Column(length = 50)
  private String emergencyContactPhone;

  @Column(columnDefinition = "TEXT")
  private String notes;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private VolunteerStatus status;

  private LocalDate startDate;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "volunteer_skill", joinColumns = @JoinColumn(name = "volunteer_id"))
  @Column(name = "skill", length = 100)
  private Set<String> skills = new LinkedHashSet<>();

  protected Volunteer() {}

  public Volunteer(
      Organization organization,
      User user,
      String firstName,
      String lastName,
      String email,
      String phone,
      String addressLine1,
      String addressLine2,
      String city,
      String state,
      String postalCode,
      String country,
      String emergencyContactName,
      String emergencyContactPhone,
      String notes,
      VolunteerStatus status,
      LocalDate startDate,
      Set<String> skills) {
    this.organization = organization;
    this.user = user;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.phone = phone;
    this.addressLine1 = addressLine1;
    this.addressLine2 = addressLine2;
    this.city = city;
    this.state = state;
    this.postalCode = postalCode;
    this.country = country;
    this.emergencyContactName = emergencyContactName;
    this.emergencyContactPhone = emergencyContactPhone;
    this.notes = notes;
    this.status = status;
    this.startDate = startDate;
    this.skills.addAll(skills);
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

  public String getPhone() {
    return phone;
  }

  public String getAddressLine1() {
    return addressLine1;
  }

  public String getAddressLine2() {
    return addressLine2;
  }

  public String getCity() {
    return city;
  }

  public String getState() {
    return state;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public String getCountry() {
    return country;
  }

  public String getEmergencyContactName() {
    return emergencyContactName;
  }

  public String getEmergencyContactPhone() {
    return emergencyContactPhone;
  }

  public String getNotes() {
    return notes;
  }

  public VolunteerStatus getStatus() {
    return status;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public Set<String> getSkills() {
    return Set.copyOf(skills);
  }

  public void changeStatus(VolunteerStatus next) {
    boolean allowed =
        switch (status) {
          case APPLICANT -> next == VolunteerStatus.ACTIVE || next == VolunteerStatus.INACTIVE;
          case ACTIVE -> next == VolunteerStatus.INACTIVE || next == VolunteerStatus.SUSPENDED;
          case SUSPENDED -> next == VolunteerStatus.ACTIVE || next == VolunteerStatus.INACTIVE;
          case INACTIVE -> next == VolunteerStatus.ACTIVE;
        };
    if (!allowed)
      throw new org.civicops.shared.exception.BusinessRuleException(
          "INVALID_VOLUNTEER_TRANSITION", "Volunteer status transition is not allowed");
    status = next;
  }

  public void update(
      String firstName,
      String lastName,
      String email,
      String phone,
      String addressLine1,
      String addressLine2,
      String city,
      String state,
      String postalCode,
      String country,
      String emergencyContactName,
      String emergencyContactPhone,
      String notes,
      Set<String> skills) {
    if (firstName != null) this.firstName = firstName;
    if (lastName != null) this.lastName = lastName;
    if (email != null) this.email = email;
    if (phone != null) this.phone = phone;
    if (addressLine1 != null) this.addressLine1 = addressLine1;
    if (addressLine2 != null) this.addressLine2 = addressLine2;
    if (city != null) this.city = city;
    if (state != null) this.state = state;
    if (postalCode != null) this.postalCode = postalCode;
    if (country != null) this.country = country;
    if (emergencyContactName != null) this.emergencyContactName = emergencyContactName;
    if (emergencyContactPhone != null) this.emergencyContactPhone = emergencyContactPhone;
    if (notes != null) this.notes = notes;
    if (skills != null) {
      this.skills.clear();
      this.skills.addAll(skills);
    }
  }
}
